package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.ShiftSwapRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ShiftSwapRecordRepository {
    fun getAllRecords(): Flow<List<ShiftSwapRecord>>
    fun observeByDate(date: LocalDate): Flow<ShiftSwapRecord?>
    suspend fun getByDate(date: LocalDate): ShiftSwapRecord?
    suspend fun addSwap(
        startDate: LocalDate,
        days: Int,
        targetShiftName: String,
        shiftPattern: List<String>,
        originalScheduleProvider: suspend (LocalDate) -> String?
    )
    suspend fun deleteByGroupId(groupId: String)
    suspend fun deleteByDate(date: LocalDate)
}
