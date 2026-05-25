package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.LateHolidayRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LateHolidayRecordRepository {
    fun getAllRecords(): Flow<List<LateHolidayRecord>>
    fun observeByDate(date: LocalDate): Flow<LateHolidayRecord?>
    suspend fun addLateHoliday(
        startDate: LocalDate,
        days: Int,
        lateHolidayTypeId: Long,
        lateHolidayName: String,
        shortName: String
    )
    suspend fun deleteByDate(date: LocalDate)
}
