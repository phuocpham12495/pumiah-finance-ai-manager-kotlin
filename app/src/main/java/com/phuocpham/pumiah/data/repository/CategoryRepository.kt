package com.phuocpham.pumiah.data.repository

import com.phuocpham.pumiah.data.model.Category
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val client: SupabaseClient) {

    suspend fun getCategories(): Result<List<Category>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        // RLS allows: user_id = auth.uid() OR user_id IS NULL (system defaults)
        client.postgrest["categories"]
            .select()
            .decodeList<Category>()
    }

    suspend fun createCategory(
        name: String,
        color: String,
        icon: String,
        type: String
    ): Result<Category> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        val body = buildJsonObject {
            put("user_id", userId)
            put("name", name)
            put("color", color)
            put("icon", icon)
            put("type", type)
        }
        client.postgrest["categories"]
            .insert(body) { select() }
            .decodeSingle<Category>()
    }

    suspend fun updateCategory(
        id: String,
        name: String,
        color: String,
        icon: String
    ): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("name", name)
            put("color", color)
            put("icon", icon)
        }
        client.postgrest["categories"]
            .update(body) { filter { eq("id", id) } }
    }

    suspend fun deleteCategory(id: String): Result<Unit> = runCatching {
        client.postgrest["categories"]
            .delete { filter { eq("id", id) } }
    }
}
