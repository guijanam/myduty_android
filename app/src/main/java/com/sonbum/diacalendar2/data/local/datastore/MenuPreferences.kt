package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.menuDataStore: DataStore<Preferences> by preferencesDataStore(name = "menu_preferences")

class MenuPreferences(private val context: Context) {

    companion object {
        private val SELECTED_CAFETERIA = stringPreferencesKey("selected_cafeteria")
    }

    val selectedCafeteria: Flow<String?> = context.menuDataStore.data
        .map { preferences ->
            preferences[SELECTED_CAFETERIA]
        }

    suspend fun setSelectedCafeteria(name: String) {
        context.menuDataStore.edit { preferences ->
            preferences[SELECTED_CAFETERIA] = name
        }
    }
}
