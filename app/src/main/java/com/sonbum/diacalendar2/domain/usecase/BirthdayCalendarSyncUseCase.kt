package com.sonbum.diacalendar2.domain.usecase

import com.sonbum.diacalendar2.data.local.datastore.BirthdayPreferences
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.repository.BirthdayRepository
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BirthdayCalendarSyncUseCase(
    private val birthdayRepository: BirthdayRepository,
    private val deviceCalendarRepository: DeviceCalendarRepository,
    private val preferences: BirthdayPreferences
) {
    data class SyncResult(val eventCount: Int)

    suspend fun sync(): Result<SyncResult> = runCatching {
        val settings = preferences.defaults.first()
        if (!settings.calendarSyncEnabled || settings.calendarId <= 0) return@runCatching SyncResult(0)
        val calendarId = settings.calendarId
        deviceCalendarRepository.deleteEventsByMarker(calendarId, MARKER)

        val people = birthdayRepository.getPeopleOnce().associateBy { it.id }
        val currentYear = LocalDate.now().year
        var count = 0
        for (year in currentYear..currentYear + FUTURE_YEARS) {
            birthdayRepository.getOccurrencesForYear(year)
                .filter { people[it.personId]?.calendarSyncEnabled == true }
                .forEach { occurrence ->
                    val title = "🎂 ${occurrence.personName} · 만 ${occurrence.ageTurning}세"
                    if (deviceCalendarRepository.createEvent(allDay(calendarId, occurrence.date, title)) != null) count++
                }
        }
        val start = LocalDate.of(currentYear, 1, 1)
        val end = LocalDate.of(currentYear + FUTURE_YEARS, 12, 31)
        birthdayRepository.getMilestoneOccurrences()
            .filter { it.date in start..end && people[it.personId]?.calendarSyncEnabled == true }
            .forEach { occurrence ->
                val title = "✨ ${occurrence.personName} · ${occurrence.milestoneName}"
                if (deviceCalendarRepository.createEvent(allDay(calendarId, occurrence.date, title)) != null) count++
            }
        SyncResult(count)
    }

    suspend fun clear(): Result<Unit> = runCatching {
        val id = preferences.defaults.first().calendarId
        clear(id).getOrThrow()
    }

    suspend fun clear(calendarId: Long): Result<Unit> = runCatching {
        if (calendarId > 0) deviceCalendarRepository.deleteEventsByMarker(calendarId, MARKER)
    }

    private fun allDay(calendarId: Long, date: LocalDate, title: String) = CalendarEvent(
        id = 0,
        calendarId = calendarId,
        title = title,
        description = MARKER,
        location = "",
        startTime = LocalDateTime.of(date, LocalTime.MIDNIGHT),
        endTime = LocalDateTime.of(date.plusDays(1), LocalTime.MIDNIGHT),
        isAllDay = true,
        color = 0,
        rrule = null
    )

    companion object {
        const val MARKER = "DiaCalendar 생일 동기화"
        const val FUTURE_YEARS = 5
    }
}
