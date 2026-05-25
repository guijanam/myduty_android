package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.ShiftInputRecord
import com.sonbum.diacalendar2.domain.model.ShiftInputType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ShiftInputRecordRepository {
    fun getAllRecords(): Flow<List<ShiftInputRecord>>
    fun observeByDate(date: LocalDate): Flow<ShiftInputRecord?>
    suspend fun getByDate(date: LocalDate): ShiftInputRecord?

    /**
     * 충당 추가
     * @param startDate 시작 날짜
     * @param days 충당 일수 (1~2일)
     * @param shiftInputType 충당 종류
     * @param targetShiftName 교체할 교번
     * @param shiftPattern 교번 패턴
     * @param originalScheduleProvider 원래 교번 조회 함수
     */
    suspend fun addShiftInput(
        startDate: LocalDate,
        days: Int,
        shiftInputType: ShiftInputType,
        targetShiftName: String,
        shiftPattern: List<String>,
        originalScheduleProvider: suspend (LocalDate) -> String?
    )

    suspend fun deleteByDate(date: LocalDate)
    suspend fun deleteByGroupId(groupId: String)
}
