package com.sonbum.diacalendar2.domain.util

import com.sonbum.diacalendar2.domain.model.CalendarType
import com.sonbum.diacalendar2.domain.model.BirthdayMilestone
import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import com.sonbum.diacalendar2.domain.model.Feb29Policy
import com.sonbum.diacalendar2.domain.model.MilestoneRuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BirthdayCalculatorsTest {
    private val resolver = BirthdayDateResolver()

    @Test
    fun `age metrics are correct immediately before and on birthday`() {
        val person = person(year = 2000, month = 10, day = 1)
        val calculator = AgeCalculator(resolver)

        val before = calculator.snapshot(person, LocalDate.of(2026, 9, 30))!!
        assertEquals(25, before.fullAge)
        assertEquals(27, before.countingAge)
        assertEquals(26, before.yearAge)
        assertEquals(311, before.monthsSinceBirth)
        assertEquals(9495, before.daysSinceBirth)
        assertEquals(26, before.ageTurningThisYear)

        val birthday = calculator.snapshot(person, LocalDate.of(2026, 10, 1))!!
        assertEquals(26, birthday.fullAge)
    }

    @Test
    fun `completed month count does not advance at the start of a calendar month`() {
        val calculator = AgeCalculator(resolver)
        val baby = person(year = 2026, month = 1, day = 31)

        assertEquals(0, calculator.snapshot(baby, LocalDate.of(2026, 2, 1))!!.monthsSinceBirth)
        assertEquals(1, calculator.snapshot(baby, LocalDate.of(2026, 3, 1))!!.monthsSinceBirth)
    }

    @Test
    fun `february 29 policies resolve independently`() {
        val base = person(year = 2000, month = 2, day = 29)
        assertEquals(
            LocalDate.of(2025, 2, 28),
            resolver.birthdayInYear(base.copy(feb29Policy = Feb29Policy.FEBRUARY_28), 2025)
        )
        assertEquals(
            LocalDate.of(2025, 3, 1),
            resolver.birthdayInYear(base.copy(feb29Policy = Feb29Policy.MARCH_1), 2025)
        )
        assertNull(resolver.birthdayInYear(base.copy(feb29Policy = Feb29Policy.LEAP_YEARS_ONLY), 2025))
        assertEquals(LocalDate.of(2024, 2, 29), resolver.birthdayInYear(base, 2024))
        assertEquals(
            LocalDate.of(2032, 2, 29),
            resolver.nextBirthday(
                base.copy(feb29Policy = Feb29Policy.LEAP_YEARS_ONLY),
                LocalDate.of(2028, 3, 1)
            )
        )
    }

    @Test
    fun `lunar birthday converts separately for each year`() {
        val lunarNewYear = person(
            year = 2000, month = 1, day = 1,
            calendarType = CalendarType.LUNAR
        )
        assertEquals(LocalDate.of(2024, 2, 10), resolver.birthdayInYear(lunarNewYear, 2024))
        assertEquals(LocalDate.of(2025, 1, 29), resolver.birthdayInYear(lunarNewYear, 2025))
    }

    @Test
    fun `person timezone controls today`() {
        val fixed = Clock.fixed(Instant.parse("2026-09-30T15:30:00Z"), ZoneId.of("UTC"))
        val calculator = AgeCalculator(resolver, fixed)
        assertEquals(LocalDate.of(2026, 10, 1), calculator.todayFor(person(timeZone = "Asia/Seoul")))
        assertEquals(LocalDate.of(2026, 9, 30), calculator.todayFor(person(timeZone = "America/Los_Angeles")))
    }

    @Test
    fun `default and custom milestone dates follow their rule`() {
        val person = person(year = 2020, month = 1, day = 1).copy(id = 7, name = "아이")
        val calculator = MilestoneCalculator(resolver)
        val hundredDays = calculator.occurrence(
            person,
            BirthdayMilestone(1, person.id, "백일", MilestoneRuleType.DAYS_AFTER_BIRTH, 100)
        )
        val firstBirthday = calculator.occurrence(
            person,
            BirthdayMilestone(2, person.id, "첫돌", MilestoneRuleType.FULL_AGE, 1)
        )
        val countingAge = calculator.occurrence(
            person,
            BirthdayMilestone(3, person.id, "세는나이 10세", MilestoneRuleType.COUNTING_AGE_YEAR, 10)
        )

        assertEquals(LocalDate.of(2020, 4, 9), hundredDays?.date)
        assertEquals(LocalDate.of(2021, 1, 1), firstBirthday?.date)
        assertEquals(LocalDate.of(2029, 1, 1), countingAge?.date)
    }

    private fun person(
        year: Int = 2000,
        month: Int = 1,
        day: Int = 1,
        calendarType: CalendarType = CalendarType.SOLAR,
        timeZone: String = "Asia/Seoul"
    ) = BirthdayPerson(
        name = "테스트",
        birthYear = year,
        birthMonth = month,
        birthDay = day,
        calendarType = calendarType,
        timeZoneId = timeZone
    )
}
