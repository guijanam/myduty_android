package com.sonbum.diacalendar2.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(private val context: Context) {

    companion object {
        const val EXTRA_TYPE = "alarm_type"
        const val EXTRA_MEMO_ID = "memo_id"
        const val EXTRA_MEMO_TITLE = "memo_title"
        const val EXTRA_MEMO_CONTENT = "memo_content"
        const val EXTRA_DATE_STRING = "date_string"
        const val EXTRA_SHIFT_NAME = "shift_name"
        const val EXTRA_SLOT = "alarm_slot"          // 출근/전반/후반 구분
        const val EXTRA_FULL_SCREEN = "alarm_full_screen"
        const val EXTRA_SOUND = "alarm_sound"
        const val EXTRA_VIBRATE = "alarm_vibrate"
        const val EXTRA_SNOOZE_MINUTES = "alarm_snooze_minutes"

        const val TYPE_MEMO = "memo"
        const val TYPE_SHIFT = "shift"

        // 근무 알람 슬롯 (request code 분리에 사용)
        const val SLOT_COMMUTE = 0   // 출근 (workTime)
        const val SLOT_FIRST = 1     // 전반사업 (firstTime)
        const val SLOT_SECOND = 2    // 후반사업 (secondTime)

        private const val MEMO_REQUEST_CODE_BASE = 30000
        private const val SHIFT_REQUEST_CODE_BASE = 40000
        private const val SHIFT_SLOT_STRIDE = 100000  // 슬롯별 코드 공간 분리
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleMemoAlarm(
        memoId: String,
        title: String,
        content: String,
        dateString: String,
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_MEMO)
            putExtra(EXTRA_MEMO_ID, memoId)
            putExtra(EXTRA_MEMO_TITLE, title)
            putExtra(EXTRA_MEMO_CONTENT, content)
            putExtra(EXTRA_DATE_STRING, dateString)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MEMO_REQUEST_CODE_BASE + memoId.hashCode().and(0xFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerAtMillis, pendingIntent)
    }

    fun cancelMemoAlarm(memoId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MEMO_REQUEST_CODE_BASE + memoId.hashCode().and(0xFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleShiftAlarm(
        dateString: String,
        shiftName: String,
        triggerAtMillis: Long,
        slot: Int = SLOT_COMMUTE,
        fullScreen: Boolean = true,
        sound: Boolean = true,
        vibrate: Boolean = true,
        snoozeMinutes: Int = 5
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_SHIFT)
            putExtra(EXTRA_DATE_STRING, dateString)
            putExtra(EXTRA_SHIFT_NAME, shiftName)
            putExtra(EXTRA_SLOT, slot)
            putExtra(EXTRA_FULL_SCREEN, fullScreen)
            putExtra(EXTRA_SOUND, sound)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            shiftRequestCode(dateString, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerAtMillis, pendingIntent)
    }

    fun cancelShiftAlarm(dateString: String, slot: Int = SLOT_COMMUTE) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            shiftRequestCode(dateString, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun shiftRequestCode(dateString: String, slot: Int): Int =
        SHIFT_REQUEST_CODE_BASE + slot * SHIFT_SLOT_STRIDE + dateString.hashCode().and(0xFFFF)

    private fun scheduleExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
