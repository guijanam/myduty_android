package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Holiday
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HolidayRepository {
    fun getAllHolidays(): Flow<List<Holiday>>
    fun getHolidayDates(): Flow<Set<LocalDate>>
    fun getHolidayMap(): Flow<Map<LocalDate, String>>
    suspend fun isHoliday(date: LocalDate): Boolean
    suspend fun refreshHolidays(): Result<Int>

    // 사용자 공휴일 관리
    suspend fun getHolidayByDate(date: LocalDate): Holiday?
    suspend fun addUserHoliday(date: LocalDate, name: String)
    suspend fun updateHoliday(holiday: Holiday)
    suspend fun deleteHoliday(id: String)
}
