package com.sonbum.diacalendar2.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftReminderWorkerTest {

    @Test
    fun `사용자 지정 시각이 있으면 기본 계산값보다 우선한다`() {
        assertEquals(
            2_000L,
            resolveShiftAlarmTrigger(
                defaultTriggerAtMillis = 1_000L,
                customTriggerAtMillis = 2_000L
            )
        )
    }

    @Test
    fun `사용자 지정 시각이 없으면 기본 계산값을 사용한다`() {
        assertEquals(
            1_000L,
            resolveShiftAlarmTrigger(
                defaultTriggerAtMillis = 1_000L,
                customTriggerAtMillis = null
            )
        )
    }
}
