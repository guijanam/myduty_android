package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.coworkerDataStore: DataStore<Preferences> by preferencesDataStore(name = "coworker_preferences")

class CoworkerPreferences(private val context: Context) {

    companion object {
        private val SELECTED_GROUP_ID = longPreferencesKey("selected_group_id")
        private const val ALL_GROUPS = -1L
    }

    val selectedGroupId: Flow<Long?> = context.coworkerDataStore.data
        .map { preferences ->
            val value = preferences[SELECTED_GROUP_ID] ?: ALL_GROUPS
            if (value == ALL_GROUPS) null else value
        }

    suspend fun setSelectedGroupId(groupId: Long?) {
        context.coworkerDataStore.edit { preferences ->
            preferences[SELECTED_GROUP_ID] = groupId ?: ALL_GROUPS
        }
    }
}
