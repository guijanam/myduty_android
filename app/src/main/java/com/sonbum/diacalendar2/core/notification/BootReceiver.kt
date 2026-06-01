package com.sonbum.diacalendar2.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sonbum.diacalendar2.data.local.dao.MemoDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.sonbum.diacalendar2.widget.MidnightWidgetWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val memoDao: MemoDao by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleMemoAlarms()
                ShiftReminderWorker.enqueue(context)  // 근무 알람 3종 재등록
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
}
