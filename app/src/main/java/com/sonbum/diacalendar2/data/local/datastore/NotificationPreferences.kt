package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

/** 기존 출근 알람 호환용 (워커가 참조) */
data class ShiftReminderPrefs(
    val enabled: Boolean = false,
    val minutesBefore: Int = 60
)

/**
 * 근무 알람 설정.
 * - 출근(workTime) / 전반사업(firstTime) / 후반사업(secondTime) 3종 각각 on/off + 분
 * - 공통 강도: 소리 / 진동 / 풀스크린
 */
data class WorkAlarmPrefs(
    val commuteEnabled: Boolean = false,
    val commuteMinutesBefore: Int = 60,
    val firstEnabled: Boolean = false,
    val firstMinutesBefore: Int = 0,
    val secondEnabled: Boolean = false,
    val secondMinutesBefore: Int = 0,
    val fullScreen: Boolean = true,
    val sound: Boolean = true,
    val vibrate: Boolean = true
) {
    /** 하나라도 켜져 있으면 워커 등록 필요 */
    val anyEnabled: Boolean get() = commuteEnabled || firstEnabled || secondEnabled
}

class NotificationPreferences(private val context: Context) {

    companion object {
        // 기존 출근 알람 호환
        private val SHIFT_REMINDER_ENABLED = booleanPreferencesKey("shift_reminder_enabled")
        private val SHIFT_REMINDER_MINUTES_BEFORE = intPreferencesKey("shift_reminder_minutes_before")

        // 근무 알람 3종 + 강도
        private val COMMUTE_ENABLED = booleanPreferencesKey("work_alarm_commute_enabled")
        private val COMMUTE_MINUTES = intPreferencesKey("work_alarm_commute_minutes")
        private val FIRST_ENABLED = booleanPreferencesKey("work_alarm_first_enabled")
        private val FIRST_MINUTES = intPreferencesKey("work_alarm_first_minutes")
        private val SECOND_ENABLED = booleanPreferencesKey("work_alarm_second_enabled")
        private val SECOND_MINUTES = intPreferencesKey("work_alarm_second_minutes")
        private val FULL_SCREEN = booleanPreferencesKey("work_alarm_full_screen")
        private val SOUND = booleanPreferencesKey("work_alarm_sound")
        private val VIBRATE = booleanPreferencesKey("work_alarm_vibrate")
    }

    // ─── 기존 출근 알람 호환 (워커가 fallback으로 참조) ────────────────────────
    val shiftReminderPrefs: Flow<ShiftReminderPrefs> = context.notificationDataStore.data.map { p ->
        ShiftReminderPrefs(
            enabled = p[COMMUTE_ENABLED] ?: p[SHIFT_REMINDER_ENABLED] ?: false,
            minutesBefore = p[COMMUTE_MINUTES] ?: p[SHIFT_REMINDER_MINUTES_BEFORE] ?: 60
        )
    }

    // ─── 근무 알람 3종 + 강도 ─────────────────────────────────────────────────
    val workAlarmPrefs: Flow<WorkAlarmPrefs> = context.notificationDataStore.data.map { p ->
        WorkAlarmPrefs(
            commuteEnabled = p[COMMUTE_ENABLED] ?: p[SHIFT_REMINDER_ENABLED] ?: false,
            commuteMinutesBefore = p[COMMUTE_MINUTES] ?: p[SHIFT_REMINDER_MINUTES_BEFORE] ?: 60,
            firstEnabled = p[FIRST_ENABLED] ?: false,
            firstMinutesBefore = p[FIRST_MINUTES] ?: 0,
            secondEnabled = p[SECOND_ENABLED] ?: false,
            secondMinutesBefore = p[SECOND_MINUTES] ?: 0,
            fullScreen = p[FULL_SCREEN] ?: true,
            sound = p[SOUND] ?: true,
            vibrate = p[VIBRATE] ?: true
        )
    }

    suspend fun setCommute(enabled: Boolean, minutesBefore: Int) {
        context.notificationDataStore.edit {
            it[COMMUTE_ENABLED] = enabled
            it[COMMUTE_MINUTES] = minutesBefore
        }
    }

    suspend fun setFirst(enabled: Boolean, minutesBefore: Int) {
        context.notificationDataStore.edit {
            it[FIRST_ENABLED] = enabled
            it[FIRST_MINUTES] = minutesBefore
        }
    }

    suspend fun setSecond(enabled: Boolean, minutesBefore: Int) {
        context.notificationDataStore.edit {
            it[SECOND_ENABLED] = enabled
            it[SECOND_MINUTES] = minutesBefore
        }
    }

    suspend fun setIntensity(fullScreen: Boolean, sound: Boolean, vibrate: Boolean) {
        context.notificationDataStore.edit {
            it[FULL_SCREEN] = fullScreen
            it[SOUND] = sound
            it[VIBRATE] = vibrate
        }
    }
}
