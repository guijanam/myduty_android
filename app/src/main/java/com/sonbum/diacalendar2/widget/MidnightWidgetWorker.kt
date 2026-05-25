package com.sonbum.diacalendar2.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MidnightWidgetWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        WidgetUpdater.updateAll(appContext)
        scheduleNextMidnightUpdate(appContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "midnight_widget_update_work"

        fun scheduleNextMidnightUpdate(context: Context) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(10)
            val initialDelay = Duration.between(now, nextMidnight).toMillis()

            val workRequest = OneTimeWorkRequestBuilder<MidnightWidgetWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
