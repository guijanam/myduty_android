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
                val soundUri = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_URI)
                val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
                val snoozeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)

                notificationHelper.showShiftAlarm(
                    shiftName = shiftName,
                    dateString = dateString,
                    slot = slot,
                    fullScreen = fullScreen,
                    sound = sound,
                    soundUri = soundUri,
                    vibrate = vibrate,
                    snoozeMinutes = snoozeMinutes
                )
            }
        }
    }
}
