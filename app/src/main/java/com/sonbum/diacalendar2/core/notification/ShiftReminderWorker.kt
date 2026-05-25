package com.sonbum.diacalendar2.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sonbum.diacalendar2.data.local.dao.DiaDao
import com.sonbum.diacalendar2.data.local.dao.ShiftScheduleDao
import com.sonbum.diacalendar2.data.local.dao.UserShiftConfigDao
import com.sonbum.diacalendar2.data.local.datastore.NotificationPreferences
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ShiftReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val shiftScheduleDao: ShiftScheduleDao by inject()
    private val diaDao: DiaDao by inject()
    private val userShiftConfigDao: UserShiftConfigDao by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    private val notificationPreferences: NotificationPreferences by inject()

    companion object {
        const val WORK_NAME = "shift_reminder_worker"
    }

    override suspend fun doWork(): Result {
        val prefs = notificationPreferences.shiftReminderPrefs.first()
        if (!prefs.enabled) return Result.success()

        val config = userShiftConfigDao.getConfigOnce() ?: return Result.success()

        val today = LocalDate.now()
        val endDate = today.plusDays(7)
        val todayStr = today.toString()
        val endDateStr = endDate.toString()

        val schedules = shiftScheduleDao.getSchedulesBetween(todayStr, endDateStr).first()

        for (schedule in schedules) {
            val diaEntity = diaDao.getDiaByDiaIdAndOffice(schedule.shiftName, config.officeName)
            val firstTime = diaEntity?.firstTime ?: continue

            val workStartTime = try {
                LocalTime.parse(firstTime, DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                continue
            }

            val date = LocalDate.parse(schedule.date)
            val alarmDateTime = date.atTime(workStartTime)
                .minusMinutes(prefs.minutesBefore.toLong())

            val triggerMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            alarmScheduler.scheduleShiftAlarm(
                dateString = schedule.date,
                shiftName = schedule.shiftName,
                triggerAtMillis = triggerMillis
            )
        }

        return Result.success()
    }
}
