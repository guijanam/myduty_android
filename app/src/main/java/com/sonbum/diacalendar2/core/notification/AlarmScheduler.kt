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

        const val TYPE_MEMO = "memo"
        const val TYPE_SHIFT = "shift"

        private const val MEMO_REQUEST_CODE_BASE = 30000
        private const val SHIFT_REQUEST_CODE_BASE = 40000
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
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_SHIFT)
            putExtra(EXTRA_DATE_STRING, dateString)
            putExtra(EXTRA_SHIFT_NAME, shiftName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SHIFT_REQUEST_CODE_BASE + dateString.hashCode().and(0xFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerAtMillis, pendingIntent)
    }

    fun cancelShiftAlarm(dateString: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SHIFT_REQUEST_CODE_BASE + dateString.hashCode().and(0xFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

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
