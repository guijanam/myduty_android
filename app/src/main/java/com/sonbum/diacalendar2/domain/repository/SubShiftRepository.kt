package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * sub 근무(보조 교번) Repository.
 * 메인 근무(ShiftRepository)와 동일한 순환 알고리즘을 사용하되 근무순서만 관리한다.
 */
interface SubShiftRepository {
    // 사용자 설정
    fun getConfig(): Flow<UserShiftConfig?>
    suspend fun getConfigOnce(): UserShiftConfig?
    suspend fun saveConfig(config: UserShiftConfig)
    suspend fun deleteConfig()

    // 근무순서 스케줄
    fun getScheduleMap(): Flow<Map<LocalDate, String>>
    suspend fun generateAndSaveSchedules(
        shiftPattern: List<String>,
        startDate: LocalDate,
        todayShift: String,
        referenceDate: LocalDate = LocalDate.now(),
        years: Int = 3,
        todayShiftIndex: Int? = null
    ): Int
    suspend fun deleteAllSchedules()
    suspend fun deleteSchedulesFromDate(fromDate: LocalDate)
    suspend fun getScheduleCount(): Int
}
