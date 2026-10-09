package com.nexe.pdfforge.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Fetches the admin config, caches it for offline use, and sends anonymous usage counts. */
class RemoteConfigRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("remote_config", Context.MODE_PRIVATE)

    /** Last config we managed to download, or safe defaults if we never have. */
    fun cached(): RemoteConfig {
        val raw = prefs.getString(CACHE_KEY, null) ?: return RemoteConfig()
        return try {
            RemoteConfig.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            RemoteConfig()
        }
    }

    /** Downloads the latest config. Returns null when offline or on any error. */
    suspend fun fetchAndCache(): RemoteConfig? = withContext(Dispatchers.IO) {
        try {
            val text = RemoteApi.fetchConfigJson() ?: return@withContext null
            val array = JSONArray(text)
            if (array.length() == 0) return@withContext null
            val obj = array.getJSONObject(0)
            prefs.edit().putString(CACHE_KEY, obj.toString()).apply()
            RemoteConfig.fromJson(obj)
        } catch (e: Exception) {
            null
        }
    }

    /** Fire-and-forget: failures are ignored so tracking never affects the app. */
    suspend fun trackUsage(tool: String, appVersion: String) {
        withContext(Dispatchers.IO) {
            try {
                RemoteApi.postUsage(tool, appVersion)
            } catch (e: Exception) {
                // offline or blocked: ignore
            }
        }
    }

    private companion object {
        const val CACHE_KEY = "config_json"
    }
}
