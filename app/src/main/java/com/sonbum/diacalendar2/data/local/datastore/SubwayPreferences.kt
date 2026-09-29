package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.subwayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "subway_preferences"
)

class SubwayPreferences(private val context: Context) {

    companion object {
        private val SELECTED_LINE = intPreferencesKey("selected_line")
    }

    val selectedLine: Flow<Int?> = context.subwayDataStore.data
        .map { preferences -> preferences[SELECTED_LINE] }

    suspend fun saveSelectedLine(line: Int) {
        context.subwayDataStore.edit { preferences ->
            preferences[SELECTED_LINE] = line
        }
    }
}
