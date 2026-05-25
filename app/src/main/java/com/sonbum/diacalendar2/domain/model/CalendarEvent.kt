package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 기기 캘린더 이벤트(일정)를 나타내는 도메인 모델
 */
data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean = false,
    val color: Int,
    val calendarDisplayName: String = "",
    val rrule: String? = null // 반복 규칙 (RRULE 형식: "FREQ=DAILY", "FREQ=WEEKLY", "FREQ=MONTHLY", "FREQ=YEARLY")
) {
    val startDate: LocalDate get() = startTime.toLocalDate()

    /**
     * 종료 날짜를 계산
     * 종일 이벤트의 경우 CalendarProvider가 종료 시간을 다음 날 00:00:00으로 저장하므로
     * 실제 종료 날짜는 하루 전으로 계산해야 함
     */
    val endDate: LocalDate get() {
        val rawEndDate = endTime.toLocalDate()
        // 종일 이벤트이고, 종료 시간이 정확히 00:00:00인 경우 하루 전이 실제 종료일
        return if (isAllDay && endTime.hour == 0 && endTime.minute == 0 && endTime.second == 0) {
            rawEndDate.minusDays(1)
        } else {
            rawEndDate
        }
    }
}
