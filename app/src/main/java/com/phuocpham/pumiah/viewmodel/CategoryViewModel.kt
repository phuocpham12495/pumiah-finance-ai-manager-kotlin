package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.repository.CategoryRepository
import com.phuocpham.pumiah.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CategoryInsight(
    val mostFrequent: Category?,
    val mostFrequentCount: Int,
    val fastestGrowing: Category?,
    val growthPercent: Double
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Idle)
    val categories: StateFlow<UiState<List<Category>>> = _categories

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState

    private val _insight = MutableStateFlow<CategoryInsight?>(null)
    val insight: StateFlow<CategoryInsight?> = _insight

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init { loadCategories() }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = UiState.Loading
            categoryRepository.getCategories()
                .onSuccess { cats ->
                    _categories.value = UiState.Success(cats)
                    computeInsights(cats)
                }
                .onFailure { _categories.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    private suspend fun computeInsights(cats: List<Category>) {
        val now = LocalDate.now()
        val thisMonthStart = now.withDayOfMonth(1).format(fmt)
        val lastMonthStart = now.minusMonths(1).withDayOfMonth(1).format(fmt)
        val lastMonthEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth()).format(fmt)

        // Load this month's transactions
        val thisMonthTxns = transactionRepository.getTransactions(startDate = thisMonthStart, endDate = now.format(fmt))
            .getOrElse { emptyList() }.filter { it.type == "expense" }
        // Load last month's transactions
        val lastMonthTxns = transactionRepository.getTransactions(startDate = lastMonthStart, endDate = lastMonthEnd)
            .getOrElse { emptyList() }.filter { it.type == "expense" }

        // Most frequent (all time from this month)
        val countMap = thisMonthTxns.groupBy { it.categoryId }.mapValues { it.value.size }
        val mostFrequentId = countMap.maxByOrNull { it.value }?.key
        val mostFrequent = cats.find { it.id == mostFrequentId }
        val mostFrequentCount = countMap[mostFrequentId] ?: 0

        // Fastest growing (this vs last month by amount)
        val thisAmtMap = thisMonthTxns.groupBy { it.categoryId }.mapValues { e -> e.value.sumOf { it.amount } }
        val lastAmtMap = lastMonthTxns.groupBy { it.categoryId }.mapValues { e -> e.value.sumOf { it.amount } }
        var fastestGrowingId: String? = null
        var maxGrowth = Double.NEGATIVE_INFINITY
        thisAmtMap.forEach { (id, thisAmt) ->
            val lastAmt = lastAmtMap[id] ?: 0.0
            val growth = if (lastAmt > 0) (thisAmt - lastAmt) / lastAmt * 100.0 else if (thisAmt > 0) 100.0 else 0.0
            if (growth > maxGrowth) { maxGrowth = growth; fastestGrowingId = id }
        }
        val fastestGrowing = cats.find { it.id == fastestGrowingId }

        if (mostFrequent != null || fastestGrowing != null) {
            _insight.value = CategoryInsight(mostFrequent, mostFrequentCount, fastestGrowing, maxGrowth)
        }
    }

    fun createCategory(name: String, color: String, icon: String, type: String) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            categoryRepository.createCategory(name, color, icon, type)
                .onSuccess { _saveState.value = UiState.Success(Unit); loadCategories() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun updateCategory(id: String, name: String, color: String, icon: String) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            categoryRepository.updateCategory(id, name, color, icon)
                .onSuccess { _saveState.value = UiState.Success(Unit); loadCategories() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id)
                .onSuccess { loadCategories() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Không thể xóa danh mục. Danh mục đang được sử dụng trong giao dịch.") }
        }
    }

    fun resetSaveState() { _saveState.value = UiState.Idle }
}
