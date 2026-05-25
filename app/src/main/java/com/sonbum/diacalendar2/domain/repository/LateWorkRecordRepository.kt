package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.LateWorkRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LateWorkRecordRepository {
    fun getAllRecords(): Flow<List<LateWorkRecord>>
    fun observeByDate(date: LocalDate): Flow<LateWorkRecord?>
    suspend fun addLateWork(
        startDate: LocalDate,
        days: Int,
        lateWorkTypeId: Long,
        lateWorkName: String,
        shortName: String
    )
    suspend fun deleteByDate(date: LocalDate)
}
