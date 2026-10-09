package com.nexe.pdfforge.data.remote

import org.json.JSONObject

/** Settings controlled from the admin dashboard. Defaults mean "nothing special". */
data class RemoteConfig(
    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementMessage: String = "",
    val minVersionCode: Long = 1,
    val latestVersionName: String = "",
    val updateUrl: String = "",
    val updateMessage: String = "A new version of PDF Forge is available.",
    val disabledTools: Set<String> = emptySet()
) {
    companion object {
        fun fromJson(json: JSONObject): RemoteConfig {
            val tools = mutableSetOf<String>()
            val array = json.optJSONArray("disabled_tools")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val name = array.optString(i, "")
                    if (name.isNotEmpty()) tools.add(name)
                }
            }
            return RemoteConfig(
                announcementEnabled = json.optBoolean("announcement_enabled", false),
                announcementTitle = json.optString("announcement_title", ""),
                announcementMessage = json.optString("announcement_message", ""),
                minVersionCode = json.optLong("min_version_code", 1L),
                latestVersionName = json.optString("latest_version_name", ""),
                updateUrl = json.optString("update_url", ""),
                updateMessage = json.optString(
                    "update_message",
                    "A new version of PDF Forge is available."
                ),
                disabledTools = tools
            )
        }
    }
}
