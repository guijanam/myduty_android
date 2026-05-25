package com.sonbum.diacalendar2.widget

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "WidgetUpdater"

// Glance 상태 변경을 트리거하기 위한 키
private val LAST_UPDATED_KEY = longPreferencesKey("last_updated")

/**
 * 위젯 데이터가 변경되었을 때 두 위젯(DayWidget, WeekWidget)을 모두 업데이트합니다.
 * updateAppWidgetState로 상태를 변경한 뒤 update()를 호출하여
 * provideGlance()가 확실히 재실행되도록 합니다.
 */
object WidgetUpdater {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val manager = GlanceAppWidgetManager(appContext)
                val now = System.currentTimeMillis()

                // DayWidget 업데이트
                val dayWidget = DayWidget()
                val dayGlanceIds = manager.getGlanceIds(DayWidget::class.java)
                Log.d(TAG, "Updating ${dayGlanceIds.size} DayWidgets")
                dayGlanceIds.forEach { id ->
                    // 상태 변경으로 Glance에 변경 알림
                    updateAppWidgetState(appContext, id) { prefs ->
                        prefs[LAST_UPDATED_KEY] = now
                    }
                    dayWidget.update(appContext, id)
                }

                // WeekWidget 업데이트
                val weekWidget = WeekWidget()
                val weekGlanceIds = manager.getGlanceIds(WeekWidget::class.java)
                Log.d(TAG, "Updating ${weekGlanceIds.size} WeekWidgets")
                weekGlanceIds.forEach { id ->
                    updateAppWidgetState(appContext, id) { prefs ->
                        prefs[LAST_UPDATED_KEY] = now
                    }
                    weekWidget.update(appContext, id)
                }

                Log.d(TAG, "Widget update completed")
            } catch (e: Exception) {
                Log.e(TAG, "Widget update failed", e)
            }
        }
    }
}
