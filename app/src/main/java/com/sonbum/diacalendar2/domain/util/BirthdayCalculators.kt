package com.sonbum.diacalendar2.domain.util

import com.github.usingsky.calendar.KoreanLunarCalendar
import com.sonbum.diacalendar2.domain.model.AgeDisplayMode
import com.sonbum.diacalendar2.domain.model.AgeSnapshot
import com.sonbum.diacalendar2.domain.model.CalendarType
import com.sonbum.diacalendar2.domain.model.BirthdayMilestone
import com.sonbum.diacalendar2.domain.model.BirthdayOccurrence
import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import com.sonbum.diacalendar2.domain.model.Feb29Policy
import com.sonbum.diacalendar2.domain.model.LeapMonthPolicy
import com.sonbum.diacalendar2.domain.model.MilestoneOccurrence
import com.sonbum.diacalendar2.domain.model.MilestoneRuleType
import java.time.Clock
import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.time.Year
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class BirthdayDateResolver {
    fun solarBirthDate(person: BirthdayPerson): LocalDate? = when (person.calendarType) {
        CalendarType.SOLAR -> runCatching {
            LocalDate.of(person.birthYear, person.birthMonth, person.birthDay)
        }.getOrNull()
        CalendarType.LUNAR -> lunarToSolar(
            person.birthYear, person.birthMonth, person.birthDay, person.isLeapMonth
        )
    }

    fun birthdayInYear(person: BirthdayPerson, year: Int): LocalDate? =
        when (person.calendarType) {
            CalendarType.SOLAR -> solarBirthdayInYear(person, year)
            CalendarType.LUNAR -> lunarBirthdayInYear(person, year)
        }

    fun nextBirthday(person: BirthdayPerson, today: LocalDate): LocalDate? {
        val birthDate = solarBirthDate(person) ?: return null
        return (today.year - 1..today.year + 8)
            .mapNotNull { birthdayInYear(person, it) }
            .filter { !it.isBefore(today) && !it.isBefore(birthDate) }
            .minOrNull()
    }

    private fun solarBirthdayInYear(person: BirthdayPerson, year: Int): LocalDate? {
        if (person.birthMonth == 2 && person.birthDay == 29 && !Year.isLeap(year.toLong())) {
            return when (person.feb29Policy) {
                Feb29Policy.FEBRUARY_28 -> LocalDate.of(year, 2, 28)
                Feb29Policy.MARCH_1 -> LocalDate.of(year, 3, 1)
                Feb29Policy.LEAP_YEARS_ONLY -> null
            }
        }
        return runCatching { LocalDate.of(year, person.birthMonth, person.birthDay) }.getOrNull()
    }

    private fun lunarBirthdayInYear(person: BirthdayPerson, lunarYear: Int): LocalDate? {
        val leapResult = if (person.isLeapMonth) {
            lunarToSolar(lunarYear, person.birthMonth, person.birthDay, true)
        } else null
        if (leapResult != null) return leapResult
        if (person.isLeapMonth && person.leapMonthPolicy == LeapMonthPolicy.SKIP_YEAR) return null

        // 음력 큰달/작은달 차이로 30일이 없는 해는 해당 월의 마지막 날로 보정한다.
        for (day in person.birthDay downTo 1) {
            lunarToSolar(lunarYear, person.birthMonth, day, false)?.let { return it }
        }
        return null
    }

    fun lunarToSolar(year: Int, month: Int, day: Int, isLeapMonth: Boolean): LocalDate? =
        synchronized(KoreanLunarCalendar::class.java) {
            runCatching {
                val calendar = KoreanLunarCalendar.getInstance()
                if (!calendar.setLunarDate(year, month, day, isLeapMonth)) return@synchronized null
                LocalDate.of(calendar.solarYear, calendar.solarMonth, calendar.solarDay)
            }.getOrNull()
        }

    fun solarToLunarYear(date: LocalDate): Int? = synchronized(KoreanLunarCalendar::class.java) {
        runCatching {
            val calendar = KoreanLunarCalendar.getInstance()
            if (!calendar.setSolarDate(date.year, date.monthValue, date.dayOfMonth)) return@synchronized null
            calendar.lunarYear
        }.getOrNull()
    }
}

class AgeCalculator(
    private val dateResolver: BirthdayDateResolver,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun snapshot(person: BirthdayPerson, today: LocalDate = todayFor(person)): AgeSnapshot? {
        val birthDate = dateResolver.solarBirthDate(person) ?: return null
        if (today.isBefore(birthDate)) return null
        val period = Period.between(birthDate, today)
        return AgeSnapshot(
            fullAge = period.years,
            countingAge = today.year - birthDate.year + 1,
            yearAge = today.year - birthDate.year,
            monthsSinceBirth = period.toTotalMonths(),
            daysSinceBirth = ChronoUnit.DAYS.between(birthDate, today),
            ageTurningThisYear = today.year - birthDate.year
        )
    }

    fun todayFor(person: BirthdayPerson): LocalDate =
        LocalDate.now(clock.withZone(safeZone(person.timeZoneId)))

    fun displayText(person: BirthdayPerson, snapshot: AgeSnapshot? = snapshot(person)): String {
        snapshot ?: return "계산 불가"
        return when (person.ageDisplayMode) {
            AgeDisplayMode.FULL_AGE -> "만 ${snapshot.fullAge}세"
            AgeDisplayMode.COUNTING_AGE -> "세는나이 ${snapshot.countingAge}세"
            AgeDisplayMode.YEAR_AGE -> "연 나이 ${snapshot.yearAge}세"
            AgeDisplayMode.MONTHS -> "생후 ${snapshot.monthsSinceBirth}개월"
            AgeDisplayMode.DAYS -> "태어난 지 ${snapshot.daysSinceBirth}일"
        }
    }

    fun zodiac(person: BirthdayPerson): String? {
        val birthDate = dateResolver.solarBirthDate(person) ?: return null
        val lunarYear = dateResolver.solarToLunarYear(birthDate) ?: return null
        val animals = listOf("원숭이", "닭", "개", "돼지", "쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양")
        return "${animals[Math.floorMod(lunarYear, 12)]}띠"
    }

    fun westernZodiac(person: BirthdayPerson): String? {
        val date = dateResolver.solarBirthDate(person) ?: return null
        val md = MonthDay.from(date)
        return when {
            md >= MonthDay.of(12, 22) || md <= MonthDay.of(1, 19) -> "염소자리"
            md <= MonthDay.of(2, 18) -> "물병자리"
            md <= MonthDay.of(3, 20) -> "물고기자리"
            md <= MonthDay.of(4, 19) -> "양자리"
            md <= MonthDay.of(5, 20) -> "황소자리"
            md <= MonthDay.of(6, 21) -> "쌍둥이자리"
            md <= MonthDay.of(7, 22) -> "게자리"
            md <= MonthDay.of(8, 22) -> "사자자리"
            md <= MonthDay.of(9, 22) -> "처녀자리"
            md <= MonthDay.of(10, 23) -> "천칭자리"
            md <= MonthDay.of(11, 22) -> "전갈자리"
            else -> "사수자리"
        }
    }

    private fun safeZone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(clock.zone)
}

class MilestoneCalculator(private val dateResolver: BirthdayDateResolver) {
    fun occurrence(person: BirthdayPerson, milestone: BirthdayMilestone): MilestoneOccurrence? {
        if (!milestone.enabled) return null
        val birthDate = dateResolver.solarBirthDate(person) ?: return null
        val date = when (milestone.ruleType) {
            MilestoneRuleType.DAYS_AFTER_BIRTH -> birthDate.plusDays((milestone.ruleValue - 1).coerceAtLeast(0).toLong())
            MilestoneRuleType.FULL_AGE ->
                dateResolver.birthdayInYear(person, person.birthYear + milestone.ruleValue)
            MilestoneRuleType.COUNTING_AGE_YEAR ->
                LocalDate.of(person.birthYear + milestone.ruleValue - 1, 1, 1)
        } ?: return null
        return MilestoneOccurrence(
            milestoneId = milestone.id,
            personId = person.id,
            personName = person.name,
            milestoneName = milestone.name,
            date = date
        )
    }
}

fun BirthdayDateResolver.occurrence(person: BirthdayPerson, year: Int): BirthdayOccurrence? {
    val date = birthdayInYear(person, year) ?: return null
    val birthDate = solarBirthDate(person) ?: return null
    return BirthdayOccurrence(
        personId = person.id,
        personName = person.name,
        date = date,
        ageTurning = (date.year - birthDate.year).coerceAtLeast(0),
        isLunar = person.calendarType == CalendarType.LUNAR,
        isLeapMonth = person.isLeapMonth,
        timeZoneId = person.timeZoneId
    )
}
