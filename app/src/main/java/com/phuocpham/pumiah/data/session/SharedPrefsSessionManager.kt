package com.phuocpham.pumiah.data.session

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPrefsSessionManager(context: Context) : SessionManager {

    private val prefs = context.getSharedPreferences("supabase_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadSession(): UserSession? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(raw) }.getOrNull()
    }

    override suspend fun saveSession(userSession: UserSession) {
        prefs.edit().putString(KEY, json.encodeToString(userSession)).apply()
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "session"
    }
}
