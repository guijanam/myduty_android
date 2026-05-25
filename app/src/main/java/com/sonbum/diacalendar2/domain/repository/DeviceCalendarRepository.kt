package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import java.time.LocalDate

/**
 * 기기 캘린더 접근을 위한 Repository 인터페이스
 */
interface DeviceCalendarRepository {
    /**
     * 이벤트 변경 알림 Flow (CRUD 후 emit)
     */
    val eventChanges: SharedFlow<Unit>

    /**
     * 기기에 등록된 모든 캘린더 목록을 조회
     */
    suspend fun getCalendars(): List<DeviceCalendar>

    /**
     * 선택된 캘린더 ID 목록을 Flow로 반환
     */
    fun getSelectedCalendarIds(): Flow<Set<Long>>

    /**
     * 선택된 캘린더 ID 목록을 저장
     */
    suspend fun saveSelectedCalendarIds(ids: Set<Long>)

    /**
     * 특정 캘린더 선택 상태 토글
     */
    suspend fun toggleCalendarSelection(calendarId: Long)

    // ===== 이벤트 CRUD =====

    /**
     * 특정 기간의 이벤트 목록 조회 (선택된 캘린더만)
     */
    suspend fun getEvents(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent>

    /**
     * 특정 날짜의 이벤트 목록 조회 (선택된 캘린더만)
     */
    suspend fun getEventsForDate(date: LocalDate): List<CalendarEvent>

    /**
     * 이벤트 생성
     */
    suspend fun createEvent(event: CalendarEvent): Long?

    /**
     * 이벤트 수정
     */
    suspend fun updateEvent(event: CalendarEvent): Boolean

    /**
     * 이벤트 삭제
     */
    suspend fun deleteEvent(eventId: Long): Boolean

    /**
     * 특정 이벤트 조회
     */
    suspend fun getEventById(eventId: Long): CalendarEvent?
}
