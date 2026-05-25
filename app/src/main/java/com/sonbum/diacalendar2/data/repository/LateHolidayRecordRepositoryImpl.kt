package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.LateHolidayRecordDao
import com.sonbum.diacalendar2.data.local.entity.LateHolidayRecordEntity
import com.sonbum.diacalendar2.domain.model.LateHolidayRecord
import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class LateHolidayRecordRepositoryImpl(
    private val dao: LateHolidayRecordDao
) : LateHolidayRecordRepository {

    override fun getAllRecords(): Flow<List<LateHolidayRecord>> {
        return dao.getAll().map { list ->
            list.map { entity ->
                entity.toDomain(LocalDate.parse(entity.date))
            }
        }
    }

    override fun observeByDate(date: LocalDate): Flow<LateHolidayRecord?> {
        return dao.getByDate(date.toString()).map { entity ->
            entity?.toDomain(date)
        }
    }

    override suspend fun addLateHoliday(
        startDate: LocalDate,
        days: Int,
        lateHolidayTypeId: Long,
        lateHolidayName: String,
        shortName: String
    ) {
        for (i in 0 until days) {
            val date = startDate.plusDays(i.toLong())
            val entity = LateHolidayRecordEntity(
                date = date.toString(),
                lateHolidayTypeId = lateHolidayTypeId,
                lateHolidayName = lateHolidayName,
                shortName = shortName
            )
            dao.insert(entity)
        }
    }

    override suspend fun deleteByDate(date: LocalDate) {
        dao.deleteByDate(date.toString())
    }

    private fun LateHolidayRecordEntity.toDomain(dateObj: LocalDate): LateHolidayRecord {
        return LateHolidayRecord(
            id = id,
            date = dateObj,
            lateHolidayTypeId = lateHolidayTypeId,
            lateHolidayName = lateHolidayName,
            shortName = shortName
        )
    }
}
