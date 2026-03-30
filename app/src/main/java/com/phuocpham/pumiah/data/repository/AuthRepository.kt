package com.phuocpham.pumiah.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val client: SupabaseClient) {

    val currentUser get() = client.auth.currentUserOrNull()

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // Profile row is auto-created by Supabase trigger handle_new_user()
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        client.auth.signOut()
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        client.auth.resetPasswordForEmail(email)
    }

    fun isLoggedIn(): Boolean = client.auth.currentUserOrNull() != null
}
