package com.sonbum.diacalendar2.data.local

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

class OfficeWebsiteRegistry(private val context: Context) {

	private val mapping: Map<String, String> by lazy { loadMapping(ASSET_NAME) }
	private val passwordMapping: Map<String, String> by lazy { loadMapping(PASSWORD_ASSET_NAME) }
	private val variantMapping: Map<String, Map<String, String>> by lazy {
		loadVariantMapping(VARIANT_ASSET_NAME)
	}

	private val authPrefs by lazy {
		context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
	}

	fun getUrl(officeName: String?): String? {
		if (officeName.isNullOrBlank()) return null
		return mapping[officeName]
	}

	fun getPassword(officeName: String?): String? {
		if (officeName.isNullOrBlank()) return null
		return passwordMapping[officeName]
	}

	fun requiresPassword(officeName: String?): Boolean {
		return !getPassword(officeName).isNullOrEmpty()
	}

	fun isAuthenticated(officeName: String?): Boolean {
		if (officeName.isNullOrBlank()) return false
		return authPrefs.getBoolean(officeName, false)
	}

	fun setAuthenticated(officeName: String) {
		authPrefs.edit().putBoolean(officeName, true).apply()
	}

	fun verifyPassword(officeName: String?, input: String): Boolean {
		val expected = getPassword(officeName) ?: return false
		return expected == input
	}

	fun getVariantUrls(officeName: String?): Map<String, String> {
		if (officeName.isNullOrBlank()) return emptyMap()
		return variantMapping[officeName] ?: emptyMap()
	}

	private fun loadMapping(assetName: String): Map<String, String> {
		return try {
			context.assets.open(assetName).bufferedReader().use { reader ->
				Json.decodeFromString<Map<String, String>>(reader.readText())
			}
		} catch (e: Exception) {
			Log.w(TAG, "Failed to load $assetName", e)
			emptyMap()
		}
	}

	private fun loadVariantMapping(assetName: String): Map<String, Map<String, String>> {
		return try {
			context.assets.open(assetName).bufferedReader().use { reader ->
				Json.decodeFromString<Map<String, Map<String, String>>>(reader.readText())
			}
		} catch (e: Exception) {
			Log.w(TAG, "Failed to load $assetName", e)
			emptyMap()
		}
	}

	companion object {
		private const val TAG = "OfficeWebsiteRegistry"
		private const val ASSET_NAME = "office_websites.json"
		private const val PASSWORD_ASSET_NAME = "office_passwords.json"
		private const val VARIANT_ASSET_NAME = "office_website_variants.json"
		private const val AUTH_PREFS = "office_website_auth"
	}
}
