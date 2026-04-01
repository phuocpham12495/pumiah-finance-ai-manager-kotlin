package com.phuocpham.pumiah.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Sealed state wrapper ─────────────────────────────────────────────────────
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// ─── User / Profile ───────────────────────────────────────────────────────────
@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ─── Category ─────────────────────────────────────────────────────────────────
@Serializable
data class Category(
    val id: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("parent_category_id") val parentCategoryId: String? = null,
    val name: String = "",
    val color: String = "#6C5CE7",
    val icon: String = "attach_money",
    val type: String = "expense"  // "income" | "expense" | "all"
)

// ─── Transaction ──────────────────────────────────────────────────────────────
@Serializable
data class Transaction(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("wallet_id") val walletId: String? = null,
    @SerialName("category_id") val categoryId: String = "",
    val amount: Double = 0.0,
    val type: String = "expense",  // "income" | "expense"
    @SerialName("transaction_date") val transactionDate: String = "",
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Joined fields (not in DB — populated client-side)
    val category: Category? = null,
    val walletName: String? = null,
    val createdByEmail: String? = null
)

// ─── Budget ───────────────────────────────────────────────────────────────────
@Serializable
data class Budget(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("category_id") val categoryId: String = "",
    val amount: Double = 0.0,
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    // Joined fields
    val category: Category? = null,
    val spent: Double = 0.0  // computed from transactions
)

// ─── Goal ─────────────────────────────────────────────────────────────────────
@Serializable
data class Goal(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    @SerialName("target_amount") val targetAmount: Double = 0.0,
    @SerialName("current_amount") val currentAmount: Double = 0.0,
    @SerialName("target_date") val targetDate: String = "",
    val status: String = "active"  // "active" | "achieved" | "abandoned"
)

// ─── Wallet ───────────────────────────────────────────────────────────────────
@Serializable
data class Wallet(
    val id: String = "",
    val name: String = "",
    val type: String = "personal",   // "personal" | "shared"
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    // Client-side joined fields
    val participants: List<WalletParticipant> = emptyList(),
    val myRole: String = "co-owner"
)

@Serializable
data class WalletParticipant(
    @SerialName("wallet_id") val walletId: String = "",
    @SerialName("user_id") val userId: String = "",
    val role: String = "co-owner",  // "owner" | "co-owner"
    val email: String = ""        // resolved from users table, not stored in DB
)

// ─── Chat ─────────────────────────────────────────────────────────────────────
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String = "user",  // "user" | "assistant"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ─── Summary ──────────────────────────────────────────────────────────────────
data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0
)

// ─── Period filter ────────────────────────────────────────────────────────────
enum class PeriodType { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

data class PeriodFilter(
    val type: PeriodType = PeriodType.MONTHLY,
    val startDate: String = "",
    val endDate: String = ""
)
