package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

enum class CalendarType { SOLAR, LUNAR }

enum class AgeDisplayMode {
    FULL_AGE, COUNTING_AGE, YEAR_AGE, MONTHS, DAYS
}

enum class LeapMonthPolicy { REGULAR_SAME_MONTH, SKIP_YEAR }

enum class Feb29Policy { FEBRUARY_28, MARCH_1, LEAP_YEARS_ONLY }

enum class MilestoneRuleType { DAYS_AFTER_BIRTH, FULL_AGE, COUNTING_AGE_YEAR }

data class BirthdayPerson(
    val id: Long = 0,
    val name: String,
    val photoPath: String? = null,
    val relationship: String = "",
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val calendarType: CalendarType = CalendarType.SOLAR,
    val isLeapMonth: Boolean = false,
    val timeZoneId: String,
    val ageDisplayMode: AgeDisplayMode = AgeDisplayMode.FULL_AGE,
    val leapMonthPolicy: LeapMonthPolicy = LeapMonthPolicy.REGULAR_SAME_MONTH,
    val feb29Policy: Feb29Policy = Feb29Policy.FEBRUARY_28,
    val notificationEnabled: Boolean = true,
    val notificationOffsets: Set<Int> = setOf(30, 7, 3, 1, 0),
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val calendarSyncEnabled: Boolean = true,
    val groupIds: Set<Long> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BirthdayGroup(
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

data class BirthdayMilestone(
    val id: Long = 0,
    val personId: Long,
    val name: String,
    val ruleType: MilestoneRuleType,
    val ruleValue: Int,
    val enabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val isDefault: Boolean = false
)

data class BirthdayOccurrence(
    val personId: Long,
    val personName: String,
    val date: LocalDate,
    val ageTurning: Int,
    val isLunar: Boolean,
    val isLeapMonth: Boolean,
    val timeZoneId: String
)

data class MilestoneOccurrence(
    val milestoneId: Long,
    val personId: Long,
    val personName: String,
    val milestoneName: String,
    val date: LocalDate
)

data class AgeSnapshot(
    val fullAge: Int,
    val countingAge: Int,
    val yearAge: Int,
    val monthsSinceBirth: Long,
    val daysSinceBirth: Long,
    val ageTurningThisYear: Int
)
