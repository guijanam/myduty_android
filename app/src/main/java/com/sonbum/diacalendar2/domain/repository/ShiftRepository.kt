package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.ShiftSchedule
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ShiftRepository {
    // 사용자 설정
    fun getUserConfig(): Flow<UserShiftConfig?>
    suspend fun getUserConfigOnce(): UserShiftConfig?
    suspend fun saveUserConfig(config: UserShiftConfig)
    suspend fun deleteUserConfig()

    // 근무 스케줄
    fun getAllSchedules(): Flow<List<ShiftSchedule>>
    fun getSchedulesByMonth(year: Int, month: Int): Flow<List<ShiftSchedule>>
    fun getScheduleMap(): Flow<Map<LocalDate, String>>
    suspend fun getScheduleByDate(date: LocalDate): ShiftSchedule?
    fun observeScheduleByDate(date: LocalDate): Flow<ShiftSchedule?>
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

    /**
     * 저장된 근무 설정으로 서버에서 최신 근무표(dia) + 패턴을 다시 받아와
     * 시작 날짜 이후 스케줄을 재생성한다. 성공 시 생성된 스케줄 개수 반환.
     */
    suspend fun refreshScheduleFromServer(): Result<Int>
}
