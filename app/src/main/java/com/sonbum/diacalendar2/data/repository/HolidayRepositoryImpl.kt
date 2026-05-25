package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.HolidayDao
import com.sonbum.diacalendar2.data.local.entity.HolidayEntity
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.data.remote.SupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseApi
import com.sonbum.diacalendar2.domain.model.Holiday
import com.sonbum.diacalendar2.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class HolidayRepositoryImpl(
    private val holidayDao: HolidayDao,
    private val supabaseApi: SupabaseApi
) : HolidayRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getAllHolidays(): Flow<List<Holiday>> {
        return holidayDao.getAllHolidays().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getHolidayDates(): Flow<Set<LocalDate>> {
        return holidayDao.getAllHolidayDates().map { dates ->
            dates.map { LocalDate.parse(it, dateFormatter) }.toSet()
        }
    }

    override fun getHolidayMap(): Flow<Map<LocalDate, String>> {
        return holidayDao.getAllHolidays().map { entities ->
            entities.filter { it.isHoliday == "Y" }
                .associate { LocalDate.parse(it.locdate, dateFormatter) to it.dateName }
        }
    }

    override suspend fun isHoliday(date: LocalDate): Boolean {
        val dateString = date.format(dateFormatter)
        return holidayDao.getHolidayByDate(dateString) != null
    }

    override suspend fun refreshHolidays(): Result<Int> {
        return try {
            val apiKey = SupabaseConfig.apiKey
            val holidays = supabaseApi.getHolidays(
                apiKey = apiKey,
                authorization = "Bearer $apiKey"
            )

            val entities = holidays.map { dto ->
                HolidayEntity(
                    id = dto.id,
                    locdate = dto.locdate,
                    dateName = dto.dateName,
                    isHoliday = dto.isHoliday,
                    isUserCreated = false  // 서버에서 받아온 공휴일
                )
            }

            // 서버 공휴일만 삭제 (사용자 생성 공휴일 보존)
            holidayDao.deleteServerHolidays()
            holidayDao.insertAll(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== 사용자 공휴일 관리 =====

    override suspend fun getHolidayByDate(date: LocalDate): Holiday? {
        val dateString = date.format(dateFormatter)
        return holidayDao.getHolidayByDate(dateString)?.toDomain()
    }

    override suspend fun addUserHoliday(date: LocalDate, name: String) {
        val entity = HolidayEntity(
            id = "user_${UUID.randomUUID()}",
            locdate = date.format(dateFormatter),
            dateName = name,
            isHoliday = "Y",
            isUserCreated = true
        )
        holidayDao.insert(entity)
    }

    override suspend fun updateHoliday(holiday: Holiday) {
        holidayDao.insert(holiday.toEntity())
    }

    override suspend fun deleteHoliday(id: String) {
        holidayDao.deleteById(id)
    }
}
