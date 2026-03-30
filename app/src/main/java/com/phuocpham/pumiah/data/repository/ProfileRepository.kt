package com.phuocpham.pumiah.data.repository

import com.phuocpham.pumiah.data.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val client: SupabaseClient) {

    suspend fun getProfile(): Result<UserProfile> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        client.postgrest["users"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<UserProfile>()
    }

    suspend fun updateProfile(dateOfBirth: String?): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        val update = mapOf("date_of_birth" to dateOfBirth)
        client.postgrest["users"]
            .update(update) { filter { eq("id", userId) } }
    }

    suspend fun uploadAvatar(imageBytes: ByteArray, extension: String): Result<String> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        val path = "$userId.$extension"
        client.storage["avatars"].upload(path, imageBytes) { upsert = true }
        val url = client.storage["avatars"].publicUrl(path)
        // Persist url in users table
        client.postgrest["users"]
            .update(mapOf("avatar_url" to url)) { filter { eq("id", userId) } }
        url
    }
}
