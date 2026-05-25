package com.sonbum.diacalendar2.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayWidget()

    companion object {
        private const val TAG = "DayWidgetReceiver"
        const val ACTION_UPDATE_WIDGET = "com.sonbum.diacalendar2.ACTION_UPDATE_DAY_WIDGET"
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MidnightWidgetWorker.scheduleNextMidnightUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive: ${intent.action}")

        // ACTION_UPDATE_WIDGET은 커스텀 액션이므로 직접 처리
        // ACTION_APPWIDGET_UPDATE는 super.onReceive()에서 이미 처리됨
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val glanceIds = GlanceAppWidgetManager(context)
                        .getGlanceIds(DayWidget::class.java)
                    Log.d(TAG, "Updating ${glanceIds.size} DayWidgets via custom action")
                    glanceIds.forEach { glanceAppWidget.update(context, it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating DayWidget", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
