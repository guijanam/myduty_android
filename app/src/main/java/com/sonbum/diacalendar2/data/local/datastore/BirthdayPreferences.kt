package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sonbum.diacalendar2.domain.model.AgeDisplayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.birthdayDataStore: DataStore<Preferences> by preferencesDataStore("birthday_preferences")

data class BirthdayDefaults(
    val ageDisplayMode: AgeDisplayMode = AgeDisplayMode.FULL_AGE,
    val notificationOffsets: Set<Int> = setOf(30, 7, 3, 1, 0),
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val calendarSyncEnabled: Boolean = false,
    val calendarId: Long = -1L
)

class BirthdayPreferences(private val context: Context) {
    private object Keys {
        val AGE_MODE = stringPreferencesKey("default_age_display_mode")
        val OFFSETS = stringPreferencesKey("default_notification_offsets")
        val HOUR = intPreferencesKey("default_notification_hour")
        val MINUTE = intPreferencesKey("default_notification_minute")
        val CALENDAR_ENABLED = booleanPreferencesKey("birthday_calendar_sync_enabled")
        val CALENDAR_ID = longPreferencesKey("birthday_calendar_id")
    }

    val defaults: Flow<BirthdayDefaults> = context.birthdayDataStore.data.map { p ->
        BirthdayDefaults(
            ageDisplayMode = runCatching { AgeDisplayMode.valueOf(p[Keys.AGE_MODE] ?: "") }
                .getOrDefault(AgeDisplayMode.FULL_AGE),
            notificationOffsets = p[Keys.OFFSETS]?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: setOf(30, 7, 3, 1, 0),
            notificationHour = p[Keys.HOUR] ?: 9,
            notificationMinute = p[Keys.MINUTE] ?: 0,
            calendarSyncEnabled = p[Keys.CALENDAR_ENABLED] ?: false,
            calendarId = p[Keys.CALENDAR_ID] ?: -1L
        )
    }

    suspend fun saveDefaults(value: BirthdayDefaults) {
        context.birthdayDataStore.edit { p ->
            p[Keys.AGE_MODE] = value.ageDisplayMode.name
            p[Keys.OFFSETS] = value.notificationOffsets.sortedDescending().joinToString(",")
            p[Keys.HOUR] = value.notificationHour
            p[Keys.MINUTE] = value.notificationMinute
            p[Keys.CALENDAR_ENABLED] = value.calendarSyncEnabled
            p[Keys.CALENDAR_ID] = value.calendarId
        }
    }
}
