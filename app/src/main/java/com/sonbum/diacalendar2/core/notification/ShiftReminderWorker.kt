package com.sonbum.diacalendar2.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sonbum.diacalendar2.data.local.dao.ScheduledAlarmDao
import com.sonbum.diacalendar2.data.local.datastore.NotificationPreferences
import com.sonbum.diacalendar2.data.local.datastore.WorkAlarmPrefs
import com.sonbum.diacalendar2.data.local.entity.ScheduledAlarmEntity
import com.sonbum.diacalendar2.widget.data.EffectiveShiftTimes
import com.sonbum.diacalendar2.widget.data.WidgetDataProvider
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 오늘부터 WINDOW_DAYS일치 근무 알람을 재계산해 단일 출처(scheduled_alarms)와 AlarmManager에 반영한다.
 *
 * - 교번교체/충당/근태가 반영된 **유효교번** 기준으로 시각을 구한다(위젯과 동일 로직 재사용).
 * - 멱등: 변경(설정/교체/충당/근태)·부팅·자정마다 호출하면 항상 최신 상태로 덮어쓴다.
 *   같은 (date, slot)은 AlarmManager가 자동 replace, 사라진 알람은 cancel + DB 삭제.
 * - dismissed=true(사용자가 리스트에서 끈 알람)는 다시 등록하지 않는다.
 */
class ShiftReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val alarmScheduler: AlarmScheduler by inject()
    private val notificationPreferences: NotificationPreferences by inject()
    private val widgetDataProvider: WidgetDataProvider by inject()
    private val scheduledAlarmDao: ScheduledAlarmDao by inject()

    companion object {
        const val WORK_NAME = "shift_reminder_worker"
        const val WINDOW_DAYS = 5L
        private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

        /** 설정/교체/충당/근태 변경 직후 등 즉시 재등록이 필요할 때 호출 */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ShiftReminderWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        val prefs = notificationPreferences.workAlarmPrefs.first()

        val today = LocalDate.now()
        val endDate = today.plusDays(WINDOW_DAYS - 1)
        val dates = (0 until WINDOW_DAYS).map { today.plusDays(it) }

        // 윈도우 밖(과거/미래) 정리 + 해당 알람 cancel
        cleanupOutsideWindow(today.toString(), endDate.toString())

        if (!prefs.anyEnabled) {
            // 전부 꺼져 있으면 윈도우 내 알람도 모두 해제
            clearAllInWindow(dates)
            return Result.success()
        }

        val times = widgetDataProvider.loadEffectiveShiftTimes(dates)
        for (t in times) {
            applySlot(t, AlarmScheduler.SLOT_COMMUTE, t.workTime, prefs.commuteEnabled, prefs.commuteMinutesBefore, prefs)
            applySlot(t, AlarmScheduler.SLOT_FIRST, t.firstTime, prefs.firstEnabled, prefs.firstMinutesBefore, prefs)
            applySlot(t, AlarmScheduler.SLOT_SECOND, t.secondTime, prefs.secondEnabled, prefs.secondMinutesBefore, prefs)
        }

        return Result.success()
    }

    private suspend fun applySlot(
        t: EffectiveShiftTimes,
        slot: Int,
        timeStr: String?,
        slotEnabled: Boolean,
        minutesBefore: Int,
        prefs: WorkAlarmPrefs
    ) {
        val dateStr = t.date.toString()
        val time = parseTime(timeStr)

        // 끄거나 시각이 없으면: 등록 해제 + DB 제거
        if (!slotEnabled || time == null || t.effectiveShiftName == null) {
            alarmScheduler.cancelShiftAlarm(dateStr, slot)
            scheduledAlarmDao.delete(dateStr, slot)
            return
        }

        val triggerMillis = t.date.atTime(time)
            .minusMinutes(minutesBefore.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // dismissed 상태 보존 (사용자가 개별로 끈 알람)
        val dismissed = scheduledAlarmDao.getByDateSlot(dateStr, slot)?.dismissed ?: false

        scheduledAlarmDao.upsert(
            ScheduledAlarmEntity(
                date = dateStr,
                slot = slot,
                shiftName = t.effectiveShiftName,
                timeText = time.format(HHMM),
                triggerAtMillis = triggerMillis,
                dismissed = dismissed
            )
        )

        if (dismissed) {
            alarmScheduler.cancelShiftAlarm(dateStr, slot)  // 꺼진 알람은 실제 등록 안 함
        } else {
            alarmScheduler.scheduleShiftAlarm(
                dateString = dateStr,
                shiftName = t.effectiveShiftName,
                triggerAtMillis = triggerMillis,
                slot = slot,
                fullScreen = prefs.fullScreen,
                sound = prefs.sound,
                vibrate = prefs.vibrate
            )
        }
    }

    private suspend fun cleanupOutsideWindow(fromDate: String, toDate: String) {
        // DB의 윈도우 밖 항목을 cancel 후 삭제
        scheduledAlarmDao.getAllOnce()
            .filter { it.date < fromDate || it.date > toDate }
            .forEach { alarmScheduler.cancelShiftAlarm(it.date, it.slot) }
        scheduledAlarmDao.deleteOutsideWindow(fromDate, toDate)
    }

    private suspend fun clearAllInWindow(dates: List<LocalDate>) {
        for (date in dates) {
            for (slot in intArrayOf(
                AlarmScheduler.SLOT_COMMUTE, AlarmScheduler.SLOT_FIRST, AlarmScheduler.SLOT_SECOND
            )) {
                alarmScheduler.cancelShiftAlarm(date.toString(), slot)
                scheduledAlarmDao.delete(date.toString(), slot)
            }
        }
    }

    private fun parseTime(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalTime.parse(value.trim(), HHMM)
        } catch (e: Exception) {
            null
        }
    }
}
