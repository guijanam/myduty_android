package com.sonbum.diacalendar2.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import com.sonbum.diacalendar2.domain.repository.BirthdayRepository
import com.sonbum.diacalendar2.domain.util.BirthdayDateResolver
import com.sonbum.diacalendar2.domain.util.MilestoneCalculator
import com.sonbum.diacalendar2.domain.util.occurrence
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BirthdayReminderPayload(
    val personId: Long,
    val personName: String,
    val eventName: String,
    val eventDate: LocalDate,
    val daysBefore: Int,
    val triggerAt: Instant
)

object BirthdayReminderTimeCalculator {
    fun triggerAt(person: BirthdayPerson, eventDate: LocalDate, daysBefore: Int): Instant {
        val zone = runCatching { ZoneId.of(person.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        return eventDate.minusDays(daysBefore.toLong())
            .atTime(person.notificationHour, person.notificationMinute)
            .atZone(zone)
            .toInstant()
    }
}

class BirthdayReminderScheduler(
    private val context: Context,
    private val repository: BirthdayRepository,
    private val dateResolver: BirthdayDateResolver,
    private val clock: Clock = Clock.systemUTC()
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAll() {
        repository.getPeopleOnce().forEach { scheduleNext(it.id) }
    }

    suspend fun scheduleNext(personId: Long) {
        cancel(personId)
        val person = repository.getPerson(personId) ?: return
        if (!person.notificationEnabled) return
        val next = nextPayload(person) ?: return
        val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PERSON_ID, next.personId)
            putExtra(EXTRA_PERSON_NAME, next.personName)
            putExtra(EXTRA_EVENT_NAME, next.eventName)
            putExtra(EXTRA_EVENT_DATE, next.eventDate.toString())
            putExtra(EXTRA_DAYS_BEFORE, next.daysBefore)
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode(personId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val millis = next.triggerAt.toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    fun cancel(personId: Long) {
        val pending = PendingIntent.getBroadcast(
            context, requestCode(personId), Intent(context, BirthdayAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let { alarmManager.cancel(it) }
    }

    internal suspend fun nextPayload(person: BirthdayPerson): BirthdayReminderPayload? {
        val now = clock.instant()
        val zone = runCatching { ZoneId.of(person.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val localYear = LocalDate.now(clock.withZone(zone)).year
        val candidates = mutableListOf<BirthdayReminderPayload>()

        for (year in localYear - 1..localYear + 8) {
            val occurrence = dateResolver.occurrence(person, year) ?: continue
            person.notificationOffsets.forEach { offset ->
                val trigger = BirthdayReminderTimeCalculator.triggerAt(person, occurrence.date, offset)
                if (trigger.isAfter(now)) {
                    candidates += BirthdayReminderPayload(
                        person.id, person.name, "생일", occurrence.date, offset, trigger
                    )
                }
            }
        }

        repository.observeMilestones(person.id).first().filter { it.enabled && it.notificationEnabled }
            .forEach { milestone ->
                val occurrence = MilestoneCalculator(dateResolver).occurrence(person, milestone) ?: return@forEach
                person.notificationOffsets.forEach { offset ->
                    val trigger = BirthdayReminderTimeCalculator.triggerAt(person, occurrence.date, offset)
                    if (trigger.isAfter(now)) {
                        candidates += BirthdayReminderPayload(
                            person.id, person.name, occurrence.milestoneName, occurrence.date, offset, trigger
                        )
                    }
                }
            }
        return candidates.minByOrNull { it.triggerAt }
    }

    private fun requestCode(personId: Long): Int = REQUEST_BASE + (personId % 100_000).toInt()

    companion object {
        const val EXTRA_PERSON_ID = "birthday_person_id"
        const val EXTRA_PERSON_NAME = "birthday_person_name"
        const val EXTRA_EVENT_NAME = "birthday_event_name"
        const val EXTRA_EVENT_DATE = "birthday_event_date"
        const val EXTRA_DAYS_BEFORE = "birthday_days_before"
        private const val REQUEST_BASE = 600_000
    }
}
