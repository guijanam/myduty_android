package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.VacationRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface VacationRecordRepository {
    fun getAllRecords(): Flow<List<VacationRecord>>
    fun observeByDate(date: LocalDate): Flow<VacationRecord?>
    suspend fun getByDate(date: LocalDate): VacationRecord?
    suspend fun addVacation(startDate: LocalDate, days: Int, vacationTypeId: Long, vacationName: String, shortName: String)
    suspend fun deleteByDate(date: LocalDate)
}
