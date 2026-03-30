package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.ChatMessage
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.repository.BudgetRepository
import com.phuocpham.pumiah.data.repository.ChatRepository
import com.phuocpham.pumiah.data.repository.GoalRepository
import com.phuocpham.pumiah.data.repository.TransactionRepository
import com.phuocpham.pumiah.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _sendState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val sendState: StateFlow<UiState<Unit>> = _sendState

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        _messages.value = listOf(
            ChatMessage(
                role = "assistant",
                content = "Xin chào! Tôi là PFAM AI, trợ lý tài chính của bạn. Tôi có thể giúp bạn phân tích thu chi, đưa ra lời khuyên tiết kiệm và quản lý ngân sách. Bạn cần giúp gì không? 💰"
            )
        )
    }

    fun sendMessage(content: String) {
        val userMessage = ChatMessage(role = "user", content = content)
        _messages.value = _messages.value + userMessage
        _sendState.value = UiState.Loading

        viewModelScope.launch {
            val context = buildFinancialContext()
            chatRepository.sendMessage(content, _messages.value.dropLast(1), context)
                .onSuccess { reply ->
                    val assistantMessage = ChatMessage(role = "assistant", content = reply)
                    _messages.value = _messages.value + assistantMessage
                    _sendState.value = UiState.Idle
                }
                .onFailure {
                    val errorMessage = ChatMessage(
                        role = "assistant",
                        content = "Xin lỗi, tôi gặp lỗi khi xử lý yêu cầu của bạn. Vui lòng thử lại sau. (${it.message})"
                    )
                    _messages.value = _messages.value + errorMessage
                    _sendState.value = UiState.Error(it.message ?: "Lỗi")
                }
        }
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatMessage(role = "assistant", content = "Lịch sử chat đã được xóa. Tôi có thể giúp gì cho bạn? 💰")
        )
    }

    private fun vnd(amount: Double): String {
        val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        nf.maximumFractionDigits = 0
        return "${nf.format(amount.toLong())} VNĐ"
    }

    private suspend fun buildFinancialContext(): String {
        val now = LocalDate.now()
        val today = now.format(fmt)
        val weekStart = now.with(java.time.DayOfWeek.MONDAY).format(fmt)
        val monthStart = now.withDayOfMonth(1).format(fmt)
        val yearStart = now.withDayOfYear(1).format(fmt)
        val displayFmt = DateTimeFormatter.ofPattern("dd/MM")
        val displayFmtFull = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        return buildString {
            append("=== DỮ LIỆU TÀI CHÍNH CỦA NGƯỜI DÙNG ===\n\n")

            // Tuần này
            transactionRepository.getTransactions(startDate = weekStart, endDate = today)
                .getOrNull()?.let { txs ->
                    val income = txs.filter { it.type == "income" }.sumOf { it.amount }
                    val expense = txs.filter { it.type == "expense" }.sumOf { it.amount }
                    val weekStartDate = now.with(java.time.DayOfWeek.MONDAY)
                    append("### Tuần này (${weekStartDate.format(displayFmt)} - ${now.format(displayFmtFull)})\n")
                    append("- Thu nhập: ${vnd(income)}\n")
                    append("- Chi tiêu: ${vnd(expense)}\n")
                    append("- Số dư kỳ: ${vnd(income - expense)}\n")
                    append("- Số giao dịch: ${txs.size}\n")
                    val topExpCat = txs.filter { it.type == "expense" }
                        .groupBy { it.category?.name ?: "Khác" }
                        .mapValues { e -> e.value.sumOf { it.amount } }
                        .entries.sortedByDescending { it.value }.take(3)
                    if (topExpCat.isNotEmpty())
                        append("- Top chi tiêu: ${topExpCat.joinToString(", ") { "${it.key}: ${vnd(it.value)}" }}\n")
                    val topIncCat = txs.filter { it.type == "income" }
                        .groupBy { it.category?.name ?: "Khác" }
                        .mapValues { e -> e.value.sumOf { it.amount } }
                        .entries.sortedByDescending { it.value }.take(3)
                    if (topIncCat.isNotEmpty())
                        append("- Top thu nhập: ${topIncCat.joinToString(", ") { "${it.key}: ${vnd(it.value)}" }}\n")
                    append("\n")
                }

            // Tháng này
            transactionRepository.getTransactions(startDate = monthStart, endDate = today)
                .getOrNull()?.let { txs ->
                    val income = txs.filter { it.type == "income" }.sumOf { it.amount }
                    val expense = txs.filter { it.type == "expense" }.sumOf { it.amount }
                    append("### Tháng ${now.monthValue}/${now.year}\n")
                    append("- Thu nhập: ${vnd(income)}\n")
                    append("- Chi tiêu: ${vnd(expense)}\n")
                    append("- Số dư kỳ: ${vnd(income - expense)}\n")
                    append("- Số giao dịch: ${txs.size}\n")
                    val topExpCat = txs.filter { it.type == "expense" }
                        .groupBy { it.category?.name ?: "Khác" }
                        .mapValues { e -> e.value.sumOf { it.amount } }
                        .entries.sortedByDescending { it.value }.take(3)
                    if (topExpCat.isNotEmpty())
                        append("- Top chi tiêu: ${topExpCat.joinToString(", ") { "${it.key}: ${vnd(it.value)}" }}\n")
                    val topIncCat = txs.filter { it.type == "income" }
                        .groupBy { it.category?.name ?: "Khác" }
                        .mapValues { e -> e.value.sumOf { it.amount } }
                        .entries.sortedByDescending { it.value }.take(3)
                    if (topIncCat.isNotEmpty())
                        append("- Top thu nhập: ${topIncCat.joinToString(", ") { "${it.key}: ${vnd(it.value)}" }}\n")
                    append("\n")
                }

            // Năm nay
            transactionRepository.getSummary(yearStart, today).getOrNull()?.let { (income, expense) ->
                append("### Năm ${now.year} (đến nay)\n")
                append("- Thu nhập: ${vnd(income)}\n")
                append("- Chi tiêu: ${vnd(expense)}\n")
                append("- Số dư kỳ: ${vnd(income - expense)}\n\n")
            }

            // Giao dịch 30 ngày gần nhất (tối đa 100)
            val thirtyDaysAgo = now.minusDays(30).format(fmt)
            transactionRepository.getTransactions(startDate = thirtyDaysAgo, endDate = today)
                .getOrNull()?.take(100)?.let { transactions ->
                    if (transactions.isNotEmpty()) {
                        append("### Giao dịch 30 ngày gần nhất (tối đa 100):\n")
                        for (t in transactions) {
                            val sign = if (t.type == "income") "Thu" else "Chi"
                            append("[${t.transactionDate.take(10)}] $sign ${vnd(t.amount)} - ${t.category?.name ?: "?"}")
                            if (!t.notes.isNullOrBlank()) append(" - ${t.notes}")
                            append("\n")
                        }
                        append("\n")
                    }
                }

            // Ngân sách hiện tại
            budgetRepository.getBudgets().getOrNull()?.let { budgets ->
                if (budgets.isNotEmpty()) {
                    append("### Ngân sách hiện tại:\n")
                    for (b in budgets) {
                        val pct = if (b.amount > 0) (b.spent / b.amount * 100).toInt() else 0
                        val status = when {
                            pct >= 100 -> "⚠️ Vượt ngân sách"
                            pct >= 80  -> "⚡ Gần đến giới hạn"
                            else       -> "✅ Còn trong ngân sách"
                        }
                        append("- Danh mục: ${b.category?.name ?: "?"} | Hạn mức: ${vnd(b.amount)} | Đã dùng: ${vnd(b.spent)} ($pct%) | $status\n")
                    }
                    append("\n")
                }
            }

            // Mục tiêu tiết kiệm
            goalRepository.getGoals().getOrNull()?.let { goals ->
                val activeGoals = goals.filter { it.status == "active" }
                if (activeGoals.isNotEmpty()) {
                    append("### Mục tiêu tiết kiệm:\n")
                    for (g in activeGoals) {
                        val pct = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount * 100).toInt() else 0
                        val remaining = g.targetAmount - g.currentAmount
                        append("- \"${g.name}\" | Mục tiêu: ${vnd(g.targetAmount)} | Đã tích: ${vnd(g.currentAmount)} ($pct%) | Còn thiếu: ${vnd(remaining)} | Hạn: ${g.targetDate} | ${g.status}\n")
                    }
                    append("\n")
                }
            }

            // Ví tiền
            walletRepository.getWallets().getOrNull()?.let { wallets ->
                if (wallets.isNotEmpty()) {
                    append("### Ví tiền:\n")
                    for (w in wallets) {
                        val typeLabel = if (w.type == "shared") "Ví chung" else "Ví riêng"
                        val members = if (w.participants.isEmpty()) "Chỉ mình bạn"
                        else w.participants.joinToString(", ") { "${it.email.ifBlank { it.userId }} (${it.role})" }
                        append("- \"${w.name}\" [$typeLabel] | Thành viên: $members\n")
                    }
                    append("\n")
                }
            }

            append("=== KẾT THÚC DỮ LIỆU ===")
        }
    }
}
