package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        // 근무 동기화 ON/OFF
        private val SHIFT_SYNC_ENABLED = booleanPreferencesKey("shift_sync_enabled")
        // 근무를 등록할 전용 캘린더 ID (없으면 -1)
        private val SHIFT_SYNC_CALENDAR_ID = longPreferencesKey("shift_sync_calendar_id")
        // 이벤트 제목 괄호 안 라벨. 미설정(null) = 승무소명 기본값, 설정됨(빈문자 포함) = 사용자 지정
        private val SHIFT_SYNC_LABEL = stringPreferencesKey("shift_sync_label")
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

    // ===== 근무 캘린더 동기화 설정 =====

    /**
     * 근무 동기화 활성화 여부 Flow
     */
    val shiftSyncEnabled: Flow<Boolean> = context.calendarDataStore.data
        .map { preferences -> preferences[SHIFT_SYNC_ENABLED] ?: false }

    suspend fun setShiftSyncEnabled(enabled: Boolean) {
        context.calendarDataStore.edit { preferences ->
            preferences[SHIFT_SYNC_ENABLED] = enabled
        }
    }

    /**
     * 근무를 등록할 전용 캘린더 ID Flow (미설정 시 -1)
     */
    val shiftSyncCalendarId: Flow<Long> = context.calendarDataStore.data
        .map { preferences -> preferences[SHIFT_SYNC_CALENDAR_ID] ?: -1L }

    suspend fun setShiftSyncCalendarId(calendarId: Long) {
        context.calendarDataStore.edit { preferences ->
            preferences[SHIFT_SYNC_CALENDAR_ID] = calendarId
        }
    }

    /**
     * 이벤트 제목 괄호 안 라벨 Flow.
     * null = 미설정(승무소명 기본값 사용), "" = 라벨 없음, 그 외 = 사용자 지정 라벨.
     */
    val shiftSyncLabel: Flow<String?> = context.calendarDataStore.data
        .map { preferences -> preferences[SHIFT_SYNC_LABEL] }

    suspend fun setShiftSyncLabel(label: String) {
        context.calendarDataStore.edit { preferences ->
            preferences[SHIFT_SYNC_LABEL] = label
        }
    }

    /**
     * 라벨을 기본값(승무소명)으로 되돌림.
     */
    suspend fun clearShiftSyncLabel() {
        context.calendarDataStore.edit { preferences ->
            preferences.remove(SHIFT_SYNC_LABEL)
        }
    }
}
