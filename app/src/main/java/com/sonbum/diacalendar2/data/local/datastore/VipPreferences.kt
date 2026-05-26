package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.vipDataStore: DataStore<Preferences> by preferencesDataStore(name = "vip_preferences")

class VipPreferences(private val context: Context) {

    companion object {
        val DEVICE_VIP_STATUS = booleanPreferencesKey("device_vip_status")
        val DEVICE_VIP_LAST_CHECKED = longPreferencesKey("device_vip_last_checked")
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    }

    val deviceVipStatus: Flow<Boolean> = context.vipDataStore.data
        .map { it[DEVICE_VIP_STATUS] == true }

    val deviceVipLastChecked: Flow<Long> = context.vipDataStore.data
        .map { it[DEVICE_VIP_LAST_CHECKED] ?: 0L }

    suspend fun readSnapshot(): Pair<Boolean?, Long> {
        val prefs = context.vipDataStore.data.first()
        val status = prefs[DEVICE_VIP_STATUS]
        val lastChecked = prefs[DEVICE_VIP_LAST_CHECKED] ?: 0L
        return status to lastChecked
    }

    suspend fun saveDeviceVipStatus(isVip: Boolean) {
        context.vipDataStore.edit { prefs ->
            prefs[DEVICE_VIP_STATUS] = isVip
            prefs[DEVICE_VIP_LAST_CHECKED] = System.currentTimeMillis()
        }
    }
}
