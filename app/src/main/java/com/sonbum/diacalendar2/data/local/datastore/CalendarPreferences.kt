package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Context에 대한 DataStore 확장 프로퍼티
val Context.calendarDataStore: DataStore<Preferences> by preferencesDataStore(name = "calendar_preferences")

/**
 * 캘린더 선택 상태를 저장하는 DataStore 관리 클래스
 */
class CalendarPreferences(private val context: Context) {

    companion object {
        private val SELECTED_CALENDAR_IDS = stringSetPreferencesKey("selected_calendar_ids")
    }

    /**
     * 선택된 캘린더 ID 목록을 Flow로 반환
     */
    val selectedCalendarIds: Flow<Set<Long>> = context.calendarDataStore.data
        .map { preferences ->
            preferences[SELECTED_CALENDAR_IDS]
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
        }

    /**
     * 선택된 캘린더 ID 목록을 저장
     */
    suspend fun saveSelectedCalendarIds(ids: Set<Long>) {
        context.calendarDataStore.edit { preferences ->
            preferences[SELECTED_CALENDAR_IDS] = ids.map { it.toString() }.toSet()
        }
    }

    /**
     * 특정 캘린더 선택 상태 토글
     */
    suspend fun toggleCalendarSelection(calendarId: Long, currentSelection: Set<Long>) {
        val newSelection = if (currentSelection.contains(calendarId)) {
            currentSelection - calendarId
        } else {
            currentSelection + calendarId
        }
        saveSelectedCalendarIds(newSelection)
    }
}
