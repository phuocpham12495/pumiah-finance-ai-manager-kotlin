package com.phuocpham.pumiah.data.repository

import com.phuocpham.pumiah.data.model.Transaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val client: SupabaseClient) {

    suspend fun getTransactions(
        startDate: String? = null,
        endDate: String? = null,
        type: String? = null,
        walletId: String? = null
    ): Result<List<Transaction>> = runCatching {
        val transactions = client.postgrest["transactions"]
            .select {
                filter {
                    if (startDate != null) gte("transaction_date", startDate)
                    if (endDate != null) lte("transaction_date", endDate)
                    if (type != null) eq("type", type)
                    if (walletId != null) eq("wallet_id", walletId)
                }
                order("transaction_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(200)
            }
            .decodeList<Transaction>()

        // Fetch categories to join
        val categories = client.postgrest["categories"]
            .select()
            .decodeList<com.phuocpham.pumiah.data.model.Category>()
        val catMap = categories.associateBy { it.id }

        transactions.map { it.copy(category = catMap[it.categoryId]) }
    }

    suspend fun createTransaction(
        categoryId: String,
        amount: Double,
        type: String,
        transactionDate: String,
        notes: String?,
        walletId: String? = null
    ): Result<Transaction> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        val body = buildJsonObject {
            put("user_id", userId)
            put("category_id", categoryId)
            put("amount", amount)
            put("type", type)
            put("transaction_date", transactionDate)
            if (notes != null) put("notes", notes)
            if (walletId != null) put("wallet_id", walletId)
        }
        client.postgrest["transactions"]
            .insert(body) { select() }
            .decodeSingle<Transaction>()
    }

    suspend fun updateTransaction(
        id: String,
        categoryId: String,
        amount: Double,
        type: String,
        transactionDate: String,
        notes: String?,
        walletId: String? = null
    ): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("category_id", categoryId)
            put("amount", amount)
            put("type", type)
            put("transaction_date", transactionDate)
            if (notes != null) put("notes", notes)
            if (walletId != null) put("wallet_id", walletId)
        }
        client.postgrest["transactions"]
            .update(body) { filter { eq("id", id) } }
    }

    suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        client.postgrest["transactions"]
            .delete { filter { eq("id", id) } }
    }

    suspend fun getSummary(startDate: String, endDate: String, walletId: String? = null): Result<Pair<Double, Double>> = runCatching {
        val transactions = client.postgrest["transactions"]
            .select {
                filter {
                    gte("transaction_date", startDate)
                    lte("transaction_date", endDate)
                    if (walletId != null) eq("wallet_id", walletId)
                }
            }
            .decodeList<Transaction>()
        val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        Pair(income, expense)
    }
}
