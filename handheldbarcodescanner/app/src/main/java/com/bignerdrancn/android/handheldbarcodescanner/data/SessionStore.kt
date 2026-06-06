package com.bignerdrancn.android.handheldbarcodescanner.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class UserSession(
    val token: String,
    val userId: Long,
    val username: String,
    val displayName: String,
    val roleCode: String,
    val menuPaths: List<String>
)

data class RecentScan(
    val code: String,
    val entityType: String,
    val title: String
)

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("wms_handheld_scanner", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = preferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) {
            preferences.edit().putString(KEY_BASE_URL, normalizeBaseUrl(value)).apply()
        }

    fun loadSession(): UserSession? {
        val token = preferences.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        return UserSession(
            token = token,
            userId = preferences.getLong(KEY_USER_ID, 0L),
            username = preferences.getString(KEY_USERNAME, "") ?: "",
            displayName = preferences.getString(KEY_DISPLAY_NAME, "") ?: "",
            roleCode = preferences.getString(KEY_ROLE_CODE, "") ?: "",
            menuPaths = preferences.getString(KEY_MENU_PATHS, "")?.split("\n")?.filter { it.isNotBlank() }.orEmpty()
        )
    }

    fun saveSession(session: UserSession) {
        preferences.edit()
            .putString(KEY_TOKEN, session.token)
            .putLong(KEY_USER_ID, session.userId)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_ROLE_CODE, session.roleCode)
            .putString(KEY_MENU_PATHS, session.menuPaths.joinToString("\n"))
            .apply()
    }

    fun clearSession() {
        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_ROLE_CODE)
            .remove(KEY_MENU_PATHS)
            .apply()
    }

    fun loadScanHistory(): List<RecentScan> {
        val raw = preferences.getString(KEY_SCAN_HISTORY, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val code = item.optString("code").trim()
                    if (code.isBlank()) {
                        continue
                    }
                    add(
                        RecentScan(
                            code = code,
                            entityType = item.optString("entityType").trim(),
                            title = item.optString("title").trim().ifBlank { code }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveScanHistory(history: List<RecentScan>) {
        val array = JSONArray()
        history.take(MAX_SCAN_HISTORY).forEach { item ->
            array.put(
                JSONObject()
                    .put("code", item.code)
                    .put("entityType", item.entityType)
                    .put("title", item.title)
            )
        }
        preferences.edit().putString(KEY_SCAN_HISTORY, array.toString()).apply()
    }

    fun clearScanHistory() {
        preferences.edit().remove(KEY_SCAN_HISTORY).apply()
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:18080/api"
        const val MAX_SCAN_HISTORY = 20

        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROLE_CODE = "role_code"
        private const val KEY_MENU_PATHS = "menu_paths"
        private const val KEY_SCAN_HISTORY = "scan_history"

        fun normalizeBaseUrl(value: String): String {
            val trimmed = value.trim().trimEnd('/')
            if (trimmed.isBlank()) {
                return DEFAULT_BASE_URL
            }
            return if (trimmed.endsWith("/api")) trimmed else "$trimmed/api"
        }
    }
}
