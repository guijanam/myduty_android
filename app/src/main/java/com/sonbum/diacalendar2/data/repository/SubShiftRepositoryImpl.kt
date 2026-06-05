package com.sonbum.diacalendar2.data.repository

import android.util.Log
import com.sonbum.diacalendar2.data.local.dao.SubShiftConfigDao
import com.sonbum.diacalendar2.data.local.dao.SubShiftScheduleDao
import com.sonbum.diacalendar2.data.local.entity.SubShiftScheduleEntity
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toSubEntity
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import com.sonbum.diacalendar2.domain.repository.SubShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val TAG = "SubShiftRepository"

class SubShiftRepositoryImpl(
    private val subShiftConfigDao: SubShiftConfigDao,
    private val subShiftScheduleDao: SubShiftScheduleDao
) : SubShiftRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ===== 사용자 설정 =====

    override fun getConfig(): Flow<UserShiftConfig?> {
        return subShiftConfigDao.getConfig().map { it?.toDomain() }
    }

    override suspend fun getConfigOnce(): UserShiftConfig? {
        return subShiftConfigDao.getConfigOnce()?.toDomain()
    }

    override suspend fun saveConfig(config: UserShiftConfig) {
        Log.d(TAG, "saveConfig() - sub 근무 설정 저장: ${config.officeName}")
        subShiftConfigDao.saveConfig(config.toSubEntity())
    }

    override suspend fun deleteConfig() {
        Log.d(TAG, "deleteConfig() - sub 근무 설정 삭제")
        subShiftConfigDao.deleteConfig()
    }

    // ===== 근무순서 스케줄 =====

    override fun getScheduleMap(): Flow<Map<LocalDate, String>> {
        return subShiftScheduleDao.getAllSchedules().map { entities ->
            entities.associate {
                LocalDate.parse(it.date, dateFormatter) to it.shiftName
            }
        }
    }

    override suspend fun generateAndSaveSchedules(
        shiftPattern: List<String>,
        startDate: LocalDate,
        todayShift: String,
        referenceDate: LocalDate,
        years: Int,
        todayShiftIndex: Int?
    ): Int {
        if (shiftPattern.isEmpty()) {
            Log.e(TAG, "교번 패턴이 비어있습니다!")
            return 0
        }

        val refShiftIndex = todayShiftIndex ?: shiftPattern.indexOf(todayShift)
        if (refShiftIndex == -1 || refShiftIndex >= shiftPattern.size) {
            Log.e(TAG, "기준 근무($todayShift, index=$todayShiftIndex)를 패턴에서 찾을 수 없습니다!")
            return 0
        }

        val startDateStr = startDate.format(dateFormatter)
        subShiftScheduleDao.deleteFromDate(startDateStr)

        val daysFromRefToStart = ChronoUnit.DAYS.between(referenceDate, startDate).toInt()
        val endDate = startDate.plusYears(years.toLong())
        val schedules = mutableListOf<SubShiftScheduleEntity>()
        var currentDate = startDate
        var dayOffset = 0

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            val totalOffset = daysFromRefToStart + dayOffset
            val patternSize = shiftPattern.size
            val shiftIndex = ((refShiftIndex + totalOffset) % patternSize + patternSize) % patternSize
            schedules.add(
                SubShiftScheduleEntity(
                    date = currentDate.format(dateFormatter),
                    shiftName = shiftPattern[shiftIndex]
                )
            )
            currentDate = currentDate.plusDays(1)
            dayOffset++
        }

        schedules.chunked(1000).forEach { chunk ->
            subShiftScheduleDao.insertAll(chunk)
        }

        return subShiftScheduleDao.getCount()
    }

    override suspend fun deleteAllSchedules() {
        subShiftScheduleDao.deleteAll()
    }

    override suspend fun deleteSchedulesFromDate(fromDate: LocalDate) {
        subShiftScheduleDao.deleteFromDate(fromDate.format(dateFormatter))
    }

    override suspend fun getScheduleCount(): Int {
        return subShiftScheduleDao.getCount()
    }
}
