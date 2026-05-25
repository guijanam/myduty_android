package com.sonbum.diacalendar2.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sonbum.diacalendar2.data.local.dao.DiaDao
import com.sonbum.diacalendar2.data.local.dao.MemoDao
import com.sonbum.diacalendar2.data.local.dao.ShiftScheduleDao
import com.sonbum.diacalendar2.data.local.dao.UserShiftConfigDao
import com.sonbum.diacalendar2.data.local.datastore.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.sonbum.diacalendar2.widget.MidnightWidgetWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val memoDao: MemoDao by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    private val shiftScheduleDao: ShiftScheduleDao by inject()
    private val diaDao: DiaDao by inject()
    private val userShiftConfigDao: UserShiftConfigDao by inject()
    private val notificationPreferences: NotificationPreferences by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleMemoAlarms()
                rescheduleShiftAlarms()
                MidnightWidgetWorker.scheduleNextMidnightUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleMemoAlarms() {
        val now = System.currentTimeMillis()
        val allMemos = memoDao.getAllMemos().first()

        allMemos.filter { it.reminderEnabled && it.reminderTimeMillis != null && it.reminderTimeMillis > now }
            .forEach { memo ->
                alarmScheduler.scheduleMemoAlarm(
                    memoId = memo.objectId,
                    title = memo.title,
                    content = memo.content,
                    dateString = memo.dateString,
                    triggerAtMillis = memo.reminderTimeMillis!!
                )
            }
    }

    private suspend fun rescheduleShiftAlarms() {
        val prefs = notificationPreferences.shiftReminderPrefs.first()
        if (!prefs.enabled) return

        val config = userShiftConfigDao.getConfigOnce() ?: return

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
    }
}
