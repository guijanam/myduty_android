package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Context에 대한 DataStore 확장 프로퍼티
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * 테마 모드 정의
 */
enum class ThemeMode(val value: String, val displayName: String) {
    SYSTEM("system", "시스템 설정"),
    LIGHT("light", "라이트 모드"),
    DARK("dark", "다크 모드");

    companion object {
        fun fromValue(value: String): ThemeMode {
            return entries.find { it.value == value } ?: SYSTEM
        }
    }
}

/**
 * 테마 설정을 저장하는 DataStore 관리 클래스
 */
class ThemePreferences(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /**
     * 현재 테마 모드를 Flow로 반환
     */
    val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            val value = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.value
            ThemeMode.fromValue(value)
        }

    /**
     * 테마 모드 저장
     */
    suspend fun saveThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.value
        }
    }
}
