package com.phuocpham.pumiah.data.repository

import com.phuocpham.pumiah.data.model.Budget
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.Transaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(private val client: SupabaseClient) {

    suspend fun getBudgets(walletId: String? = null): Result<List<Budget>> = runCatching {
        val budgets = client.postgrest["budgets"]
            .select {
                filter { if (walletId != null) eq("wallet_id", walletId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<Budget>()

        val categories = client.postgrest["categories"]
            .select().decodeList<Category>()
        val catMap = categories.associateBy { it.id }

        // For each budget, compute spending from transactions (filtered by wallet if set)
        budgets.map { budget ->
            val transactions = client.postgrest["transactions"]
                .select {
                    filter {
                        eq("category_id", budget.categoryId)
                        eq("type", "expense")
                        gte("transaction_date", budget.startDate)
                        lte("transaction_date", budget.endDate)
                        if (walletId != null) eq("wallet_id", walletId)
                    }
                }
                .decodeList<Transaction>()
            val spent = transactions.sumOf { it.amount }
            budget.copy(category = catMap[budget.categoryId], spent = spent)
        }
    }

    suspend fun createBudget(
        categoryId: String,
        amount: Double,
        startDate: String,
        endDate: String,
        walletId: String? = null
    ): Result<Budget> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        val body = buildJsonObject {
            put("user_id", userId)
            put("category_id", categoryId)
            put("amount", amount)
            put("start_date", startDate)
            put("end_date", endDate)
            if (walletId != null) put("wallet_id", walletId)
        }
        client.postgrest["budgets"]
            .insert(body) { select() }
            .decodeSingle<Budget>()
    }

    suspend fun updateBudget(id: String, amount: Double): Result<Unit> = runCatching {
        val body = buildJsonObject { put("amount", amount) }
        client.postgrest["budgets"]
            .update(body) { filter { eq("id", id) } }
    }

    suspend fun deleteBudget(id: String): Result<Unit> = runCatching {
        client.postgrest["budgets"]
            .delete { filter { eq("id", id) } }
    }
}
