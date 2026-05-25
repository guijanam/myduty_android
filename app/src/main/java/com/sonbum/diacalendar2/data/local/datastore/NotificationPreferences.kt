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

data class ShiftReminderPrefs(
    val enabled: Boolean = false,
    val minutesBefore: Int = 60
)

class NotificationPreferences(private val context: Context) {

    companion object {
        private val SHIFT_REMINDER_ENABLED = booleanPreferencesKey("shift_reminder_enabled")
        private val SHIFT_REMINDER_MINUTES_BEFORE = intPreferencesKey("shift_reminder_minutes_before")
    }

    val shiftReminderPrefs: Flow<ShiftReminderPrefs> = context.notificationDataStore.data.map { prefs ->
        ShiftReminderPrefs(
            enabled = prefs[SHIFT_REMINDER_ENABLED] ?: false,
            minutesBefore = prefs[SHIFT_REMINDER_MINUTES_BEFORE] ?: 60
        )
    }

    suspend fun setShiftReminderEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { prefs ->
            prefs[SHIFT_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setShiftReminderMinutesBefore(minutes: Int) {
        context.notificationDataStore.edit { prefs ->
            prefs[SHIFT_REMINDER_MINUTES_BEFORE] = minutes
        }
    }
}
