package com.sonbum.diacalendar2.data.local

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

class DrawerWebsiteRegistry(private val context: Context) {

    private val mapping: Map<String, String> by lazy { loadMapping() }

    fun getUrl(officeName: String?): String? {
        if (officeName.isNullOrBlank()) return null
        return mapping[officeName]
    }

    private fun loadMapping(): Map<String, String> {
        return try {
            context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
                Json.decodeFromString<Map<String, String>>(reader.readText())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load $ASSET_NAME", e)
            emptyMap()
        }
    }

    companion object {
        private const val ASSET_NAME = "office_drawer_websites.json"
        private const val TAG = "DrawerWebsiteRegistry"
    }
}
