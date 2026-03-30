package com.phuocpham.pumiah.data.repository

import com.phuocpham.pumiah.data.model.Goal
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(private val client: SupabaseClient) {

    suspend fun getGoals(): Result<List<Goal>> = runCatching {
        client.postgrest["goals"]
            .select { order("target_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING) }
            .decodeList<Goal>()
    }

    suspend fun createGoal(
        name: String,
        targetAmount: Double,
        targetDate: String
    ): Result<Goal> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        val body = buildJsonObject {
            put("user_id", userId)
            put("name", name)
            put("target_amount", targetAmount)
            put("target_date", targetDate)
            put("current_amount", 0.0)
            put("status", "active")
        }
        client.postgrest["goals"]
            .insert(body) { select() }
            .decodeSingle<Goal>()
    }

    suspend fun addContribution(id: String, currentAmount: Double, newContribution: Double): Result<Unit> = runCatching {
        val newTotal = currentAmount + newContribution
        val body = buildJsonObject {
            put("current_amount", newTotal)
            if (newTotal >= currentAmount) put("status", "active")
        }
        client.postgrest["goals"]
            .update(body) { filter { eq("id", id) } }
    }

    suspend fun updateGoalStatus(id: String, status: String): Result<Unit> = runCatching {
        val body = buildJsonObject { put("status", status) }
        client.postgrest["goals"]
            .update(body) { filter { eq("id", id) } }
    }

    suspend fun deleteGoal(id: String): Result<Unit> = runCatching {
        client.postgrest["goals"]
            .delete { filter { eq("id", id) } }
    }
}
