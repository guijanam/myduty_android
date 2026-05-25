package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.crewPatternDataStore: DataStore<Preferences> by preferencesDataStore(name = "crew_pattern_preferences")

/**
 * 근무조 패턴 표시 설정을 저장하는 DataStore 관리 클래스
 */
class CrewPatternPreferences(private val context: Context) {

    companion object {
        private val SHOW_CREW_PATTERN = booleanPreferencesKey("show_crew_pattern")
        private val CREW_PATTERN = stringPreferencesKey("crew_pattern")
        private val CREW_PATTERN_START_DATE = stringPreferencesKey("crew_pattern_start_date")
    }

    val showCrewPattern: Flow<Boolean> = context.crewPatternDataStore.data
        .map { preferences ->
            preferences[SHOW_CREW_PATTERN] ?: false
        }

    val crewPattern: Flow<List<String>> = context.crewPatternDataStore.data
        .map { preferences ->
            val pattern = preferences[CREW_PATTERN] ?: "AD,BA,CB,DC"
            pattern.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }

    val crewPatternStartDate: Flow<LocalDate> = context.crewPatternDataStore.data
        .map { preferences ->
            val dateStr = preferences[CREW_PATTERN_START_DATE] ?: "2026-02-01"
            try {
                LocalDate.parse(dateStr)
            } catch (e: Exception) {
                LocalDate.of(2026, 2, 1)
            }
        }

    suspend fun saveShowCrewPattern(show: Boolean) {
        context.crewPatternDataStore.edit { preferences ->
            preferences[SHOW_CREW_PATTERN] = show
        }
    }

    suspend fun saveCrewPattern(pattern: List<String>) {
        context.crewPatternDataStore.edit { preferences ->
            preferences[CREW_PATTERN] = pattern.joinToString(",")
        }
    }

    suspend fun saveCrewPatternStartDate(date: LocalDate) {
        context.crewPatternDataStore.edit { preferences ->
            preferences[CREW_PATTERN_START_DATE] = date.toString()
        }
    }
}
