package com.sonbum.diacalendar2.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BirthdayAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val notificationHelper: NotificationHelper by inject()
    private val scheduler: BirthdayReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val personId = intent.getLongExtra(BirthdayReminderScheduler.EXTRA_PERSON_ID, -1L)
        if (personId <= 0) return
        val name = intent.getStringExtra(BirthdayReminderScheduler.EXTRA_PERSON_NAME) ?: return
        val eventName = intent.getStringExtra(BirthdayReminderScheduler.EXTRA_EVENT_NAME) ?: "생일"
        val eventDate = intent.getStringExtra(BirthdayReminderScheduler.EXTRA_EVENT_DATE) ?: return
        val days = intent.getIntExtra(BirthdayReminderScheduler.EXTRA_DAYS_BEFORE, 0)
        notificationHelper.showBirthdayNotification(personId, name, eventName, eventDate, days)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { scheduler.scheduleNext(personId) } finally { pending.finish() }
        }
    }
}
