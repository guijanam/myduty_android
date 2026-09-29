package com.sonbum.diacalendar2.core.notification

import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BirthdayReminderTimeCalculatorTest {
    @Test
    fun `person local notification time is converted to instant`() {
        val person = BirthdayPerson(
            name = "서울",
            birthYear = 2000,
            birthMonth = 10,
            birthDay = 7,
            timeZoneId = "Asia/Seoul",
            notificationHour = 9,
            notificationMinute = 0
        )

        val trigger = BirthdayReminderTimeCalculator.triggerAt(
            person = person,
            eventDate = LocalDate.of(2026, 10, 7),
            daysBefore = 7
        )

        assertEquals(Instant.parse("2026-09-30T00:00:00Z"), trigger)
    }
}
