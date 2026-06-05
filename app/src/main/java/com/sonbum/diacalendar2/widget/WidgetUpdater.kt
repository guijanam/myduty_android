package com.sonbum.diacalendar2.widget

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.sonbum.diacalendar2.core.util.DeviceIdProvider
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import com.sonbum.diacalendar2.wear.WearDataSyncManager
import com.sonbum.diacalendar2.widget.data.WidgetDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin
import java.time.LocalDate
import java.time.ZoneId

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

            // 워치(Wear OS 타일) 동기화 — 위젯과 동일하게 모든 데이터 변경 시점에 전송
            syncToWear(appContext)
        }
    }

    /**
     * 오늘의 유효교번/시각/열번을 계산해 워치 타일로 전송한다.
     * 워치 미연결/예외는 무시한다(타일은 마지막 수신 데이터를 유지).
     */
    private suspend fun syncToWear(appContext: Context) {
        try {
            val koin = getKoin()
            val provider = koin.get<WidgetDataProvider>()
            val subscriptionRepository = koin.get<SubscriptionRepository>()

            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val data = provider.loadEffectiveShiftTimes(listOf(today)).firstOrNull()

            val ssaid = DeviceIdProvider.getSsaid(appContext)
            val isPremium = subscriptionRepository.isVip(ssaid)

            WearDataSyncManager(appContext).syncTodayShift(
                date = today,
                turn = data?.effectiveShiftName,
                worktime = data?.workTime,
                firsttime = data?.firstTime,
                secondtime = data?.secondTime,
                tableName = data?.typeName,
                numtr1 = data?.numTr1,
                numtr2 = data?.numTr2,
                isPremium = isPremium
            )
        } catch (e: Exception) {
            Log.e(TAG, "Wear sync failed", e)
        }
    }
}
