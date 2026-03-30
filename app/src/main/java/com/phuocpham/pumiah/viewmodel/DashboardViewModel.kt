package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.FinancialSummary
import com.phuocpham.pumiah.data.model.Transaction
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.model.Wallet
import com.phuocpham.pumiah.data.repository.CategoryRepository
import com.phuocpham.pumiah.data.repository.TransactionRepository
import com.phuocpham.pumiah.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class DashboardPeriod { WEEK, MONTH, YEAR }

data class CategoryAmount(val name: String, val amount: Double, val color: String)
data class PeriodBar(val label: String, val income: Double, val expense: Double)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _summary = MutableStateFlow<UiState<FinancialSummary>>(UiState.Idle)
    val summary: StateFlow<UiState<FinancialSummary>> = _summary

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions: StateFlow<List<Transaction>> = _recentTransactions

    private val _expenseByCategory = MutableStateFlow<List<CategoryAmount>>(emptyList())
    val expenseByCategory: StateFlow<List<CategoryAmount>> = _expenseByCategory

    private val _incomeByCategory = MutableStateFlow<List<CategoryAmount>>(emptyList())
    val incomeByCategory: StateFlow<List<CategoryAmount>> = _incomeByCategory

    private val _periodBars = MutableStateFlow<List<PeriodBar>>(emptyList())
    val periodBars: StateFlow<List<PeriodBar>> = _periodBars

    private val _period = MutableStateFlow(DashboardPeriod.WEEK)   // default = Tuần
    val period: StateFlow<DashboardPeriod> = _period

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: StateFlow<List<Wallet>> = _wallets

    private val _selectedWalletId = MutableStateFlow<String?>(null)
    val selectedWalletId: StateFlow<String?> = _selectedWalletId

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init { loadWalletsAndData() }

    private fun loadWalletsAndData() {
        viewModelScope.launch {
            walletRepository.getWallets().onSuccess { list ->
                _wallets.value = list
                _selectedWalletId.value = defaultWallet(list)
            }
            loadData()
        }
    }

    fun setPeriod(p: DashboardPeriod) {
        _period.value = p
        loadData()
    }

    fun setWallet(walletId: String) {
        _selectedWalletId.value = walletId
        loadData()
    }

    fun loadMonthSummary() = loadData()

    fun loadData() {
        viewModelScope.launch {
            _summary.value = UiState.Loading
            val now = LocalDate.now()
            val walletId = _selectedWalletId.value
            val (start, end) = when (_period.value) {
                DashboardPeriod.WEEK -> {
                    val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    weekStart.format(fmt) to now.format(fmt)
                }
                DashboardPeriod.MONTH -> now.withDayOfMonth(1).format(fmt) to now.format(fmt)
                DashboardPeriod.YEAR  -> now.withDayOfYear(1).format(fmt) to now.format(fmt)
            }

            val categories = categoryRepository.getCategories().getOrElse { emptyList() }
            val catMap = categories.associateBy { it.id }

            transactionRepository.getTransactions(startDate = start, endDate = end, walletId = walletId)
                .onSuccess { txns ->
                    val income  = txns.filter { it.type == "income"  }.sumOf { it.amount }
                    val expense = txns.filter { it.type == "expense" }.sumOf { it.amount }
                    _summary.value = UiState.Success(FinancialSummary(income, expense, income - expense, txns.size))
                    _recentTransactions.value = txns.take(5)

                    // Expense by category
                    val expMap = mutableMapOf<String, Pair<Double, String>>()
                    txns.filter { it.type == "expense" }.forEach { t ->
                        val cat   = catMap[t.categoryId]
                        val name  = cat?.name  ?: "Khác"
                        val color = cat?.color ?: "#E17055"
                        expMap[name] = Pair((expMap[name]?.first ?: 0.0) + t.amount, color)
                    }
                    _expenseByCategory.value = expMap.map { (n, p) -> CategoryAmount(n, p.first, p.second) }
                        .sortedByDescending { it.amount }

                    // Income by category
                    val incMap = mutableMapOf<String, Pair<Double, String>>()
                    txns.filter { it.type == "income" }.forEach { t ->
                        val cat   = catMap[t.categoryId]
                        val name  = cat?.name  ?: "Khác"
                        val color = cat?.color ?: "#00B894"
                        incMap[name] = Pair((incMap[name]?.first ?: 0.0) + t.amount, color)
                    }
                    _incomeByCategory.value = incMap.map { (n, p) -> CategoryAmount(n, p.first, p.second) }
                        .sortedByDescending { it.amount }

                    _periodBars.value = buildPeriodBars(txns, _period.value, now)
                }
                .onFailure { _summary.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    companion object {
        /** Most-recent shared wallet; falls back to most-recent any wallet; null only if list is empty. */
        fun defaultWallet(list: List<com.phuocpham.pumiah.data.model.Wallet>): String? =
            (list.filter { it.type == "shared" }.maxByOrNull { it.createdAt ?: "" }
                ?: list.maxByOrNull { it.createdAt ?: "" })?.id
    }

    private fun buildPeriodBars(txns: List<Transaction>, period: DashboardPeriod, now: LocalDate): List<PeriodBar> {
        return when (period) {
            DashboardPeriod.WEEK -> {
                val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                (0..6).map { i ->
                    val day    = weekStart.plusDays(i.toLong())
                    val dayStr = day.format(fmt)
                    val label  = "${day.dayOfMonth}/${day.monthValue}"
                    PeriodBar(label,
                        txns.filter { it.type == "income"  && it.transactionDate.take(10) == dayStr }.sumOf { it.amount },
                        txns.filter { it.type == "expense" && it.transactionDate.take(10) == dayStr }.sumOf { it.amount })
                }
            }
            DashboardPeriod.MONTH -> {
                (1..now.lengthOfMonth()).mapNotNull { day ->
                    val d      = now.withDayOfMonth(day)
                    val dayStr = d.format(fmt)
                    val inc = txns.filter { it.type == "income"  && it.transactionDate.take(10) == dayStr }.sumOf { it.amount }
                    val exp = txns.filter { it.type == "expense" && it.transactionDate.take(10) == dayStr }.sumOf { it.amount }
                    if (inc > 0 || exp > 0) PeriodBar("${d.dayOfMonth}", inc, exp) else null
                }
            }
            DashboardPeriod.YEAR -> {
                (1..now.monthValue).map { month ->
                    val monthStr = String.format("%04d-%02d", now.year, month)
                    PeriodBar("$month/${now.year % 100}",
                        txns.filter { it.type == "income"  && it.transactionDate.startsWith(monthStr) }.sumOf { it.amount },
                        txns.filter { it.type == "expense" && it.transactionDate.startsWith(monthStr) }.sumOf { it.amount })
                }
            }
        }
    }
}
