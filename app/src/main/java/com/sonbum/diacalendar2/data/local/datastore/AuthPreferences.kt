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

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

class AuthPreferences(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val NICKNAME = stringPreferencesKey("nickname")
        private val EMAIL = stringPreferencesKey("email")
        private val SELECTED_CATEGORY = stringPreferencesKey("selected_board_category")
        private val GUIDELINES_AGREED = booleanPreferencesKey("guidelines_agreed")
        private val LAST_CHECKED_BOARD = stringPreferencesKey("last_checked_board")
    }

    val accessToken: Flow<String?> = context.authDataStore.data
        .map { it[ACCESS_TOKEN] }

    val refreshToken: Flow<String?> = context.authDataStore.data
        .map { it[REFRESH_TOKEN] }

    val userId: Flow<String?> = context.authDataStore.data
        .map { it[USER_ID] }

    val nickname: Flow<String?> = context.authDataStore.data
        .map { it[NICKNAME] }

    val email: Flow<String?> = context.authDataStore.data
        .map { it[EMAIL] }

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data
        .map { it[ACCESS_TOKEN] != null }

    val selectedCategory: Flow<String?> = context.authDataStore.data
        .map { it[SELECTED_CATEGORY] }

    val guidelinesAgreed: Flow<Boolean> = context.authDataStore.data
        .map { it[GUIDELINES_AGREED] == true }

    val lastCheckedBoard: Flow<String?> = context.authDataStore.data
        .map { it[LAST_CHECKED_BOARD] }

    suspend fun saveAuthSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String?
    ) {
        context.authDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            refreshToken?.let { prefs[REFRESH_TOKEN] = it }
            prefs[USER_ID] = userId
            email?.let { prefs[EMAIL] = it }
        }
    }

    suspend fun saveNickname(nickname: String) {
        context.authDataStore.edit { prefs ->
            prefs[NICKNAME] = nickname
        }
    }

    suspend fun saveSelectedCategory(categoryCode: String?) {
        context.authDataStore.edit { prefs ->
            if (categoryCode != null) {
                prefs[SELECTED_CATEGORY] = categoryCode
            } else {
                prefs.remove(SELECTED_CATEGORY)
            }
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(USER_ID)
            prefs.remove(NICKNAME)
            prefs.remove(EMAIL)
        }
    }

    suspend fun saveLastCheckedBoard(isoTimestamp: String) {
        context.authDataStore.edit { prefs ->
            prefs[LAST_CHECKED_BOARD] = isoTimestamp
        }
    }

    suspend fun saveGuidelinesAgreed() {
        context.authDataStore.edit { prefs ->
            prefs[GUIDELINES_AGREED] = true
        }
    }

    suspend fun getAccessTokenSync(): String? {
        var token: String? = null
        context.authDataStore.edit { prefs ->
            token = prefs[ACCESS_TOKEN]
        }
        return token
    }
}
