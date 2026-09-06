package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.shiftColorDataStore: DataStore<Preferences> by preferencesDataStore(name = "shift_color_preferences")

data class ShiftDisplayColors(
    val dayShiftColorHex: String = DEFAULT_DAY_SHIFT_COLOR_HEX,
    val nightShiftColorHex: String = DEFAULT_NIGHT_SHIFT_COLOR_HEX
) {
    companion object {
        /**
         * 기본값은 고정 hex가 아니라 "테마 기본색(primary 계열)"을 의미하는 빈 값이다.
         * 앱이 dynamic color를 쓰기 때문에 primary는 기기/배경화면마다 달라진다.
         * 따라서 색을 파싱할 수 없는 이 값이 내려가면 각 UI가 MaterialTheme의 primary 계열로 폴백한다.
         */
        const val DEFAULT_DAY_SHIFT_COLOR_HEX = ""
        const val DEFAULT_NIGHT_SHIFT_COLOR_HEX = ""

        val DEFAULT = ShiftDisplayColors()
    }
}

class ShiftColorPreferences(private val context: Context) {

    companion object {
        private val DAY_SHIFT_COLOR = stringPreferencesKey("day_shift_color")
        private val NIGHT_SHIFT_COLOR = stringPreferencesKey("night_shift_color")
    }

    val colors: Flow<ShiftDisplayColors> = context.shiftColorDataStore.data
        .map { preferences ->
            ShiftDisplayColors(
                dayShiftColorHex = preferences[DAY_SHIFT_COLOR]
                    ?: ShiftDisplayColors.DEFAULT_DAY_SHIFT_COLOR_HEX,
                nightShiftColorHex = preferences[NIGHT_SHIFT_COLOR]
                    ?: ShiftDisplayColors.DEFAULT_NIGHT_SHIFT_COLOR_HEX
            )
        }

    suspend fun saveDayShiftColor(hex: String) {
        context.shiftColorDataStore.edit { preferences ->
            preferences[DAY_SHIFT_COLOR] = hex
        }
    }

    suspend fun saveNightShiftColor(hex: String) {
        context.shiftColorDataStore.edit { preferences ->
            preferences[NIGHT_SHIFT_COLOR] = hex
        }
    }

    suspend fun resetToDefault() {
        context.shiftColorDataStore.edit { preferences ->
            preferences[DAY_SHIFT_COLOR] = ShiftDisplayColors.DEFAULT_DAY_SHIFT_COLOR_HEX
            preferences[NIGHT_SHIFT_COLOR] = ShiftDisplayColors.DEFAULT_NIGHT_SHIFT_COLOR_HEX
        }
    }
}
