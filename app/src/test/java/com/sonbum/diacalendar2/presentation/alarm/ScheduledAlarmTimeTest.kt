package com.sonbum.diacalendar2.presentation.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ScheduledAlarmTimeTest {

    @Test
    fun `변경한 시각은 기존 알람의 날짜를 유지한다`() {
        val zoneId = ZoneId.of("Asia/Seoul")
        val original = LocalDateTime.of(2026, 9, 15, 5, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val result = triggerAtSelectedTime(original, LocalTime.of(7, 45), zoneId)

        assertEquals(
            LocalDateTime.of(2026, 9, 15, 7, 45)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
            result
        )
    }
}
