package com.sonbum.diacalendar2.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationHelper: NotificationHelper by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: return

        when (type) {
            AlarmScheduler.TYPE_MEMO -> {
                val memoId = intent.getStringExtra(AlarmScheduler.EXTRA_MEMO_ID) ?: return
                val title = intent.getStringExtra(AlarmScheduler.EXTRA_MEMO_TITLE) ?: "메모 알림"
                val content = intent.getStringExtra(AlarmScheduler.EXTRA_MEMO_CONTENT) ?: ""
                val dateString = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_STRING) ?: return

                notificationHelper.showMemoNotification(title, content, memoId, dateString)
            }
            AlarmScheduler.TYPE_SHIFT -> {
                val shiftName = intent.getStringExtra(AlarmScheduler.EXTRA_SHIFT_NAME) ?: return
                val dateString = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_STRING) ?: return
                val slot = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT, AlarmScheduler.SLOT_COMMUTE)
                val fullScreen = intent.getBooleanExtra(AlarmScheduler.EXTRA_FULL_SCREEN, true)
                val sound = intent.getBooleanExtra(AlarmScheduler.EXTRA_SOUND, true)
                val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
                val snoozeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)

                if (fullScreen) {
                    notificationHelper.showShiftAlarm(shiftName, dateString, slot, sound, vibrate, snoozeMinutes)
                } else {
                    notificationHelper.showShiftNotificationSimple(shiftName, dateString, slot)
                }
            }
        }
    }
}
