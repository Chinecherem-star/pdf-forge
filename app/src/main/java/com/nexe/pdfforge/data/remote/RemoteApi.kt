package com.nexe.pdfforge.data.remote

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Minimal HTTPS client for the Supabase REST API (no extra libraries needed). */
object RemoteApi {

    private fun open(path: String, method: String): HttpURLConnection {
        val connection = URL(RemoteConstants.SUPABASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("apikey", RemoteConstants.SUPABASE_ANON_KEY)
        connection.setRequestProperty("Authorization", "Bearer " + RemoteConstants.SUPABASE_ANON_KEY)
        return connection
    }

    /** Returns the raw JSON array text for the single config row, or null on any failure. */
    fun fetchConfigJson(): String? {
        val connection = open("/rest/v1/app_config?id=eq.1&select=*", "GET")
        return try {
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Sends one anonymous usage event: just a tool name and the app version. */
    fun postUsage(tool: String, appVersion: String) {
        val connection = open("/rest/v1/usage_events", "POST")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Prefer", "return=minimal")
            val body = JSONObject()
                .put("tool", tool.take(40))
                .put("app_version", appVersion.take(20))
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }
}
