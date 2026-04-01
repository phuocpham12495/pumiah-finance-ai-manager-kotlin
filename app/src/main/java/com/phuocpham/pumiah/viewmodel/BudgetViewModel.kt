package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.Budget
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.model.Wallet
import com.phuocpham.pumiah.data.repository.BudgetRepository
import com.phuocpham.pumiah.data.repository.CategoryRepository
import com.phuocpham.pumiah.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _budgets = MutableStateFlow<UiState<List<Budget>>>(UiState.Idle)
    val budgets: StateFlow<UiState<List<Budget>>> = _budgets

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: StateFlow<List<Wallet>> = _wallets

    private val _walletsLoading = MutableStateFlow(true)
    val walletsLoading: StateFlow<Boolean> = _walletsLoading

    private val _selectedWalletId = MutableStateFlow<String?>(null)
    val selectedWalletId: StateFlow<String?> = _selectedWalletId

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState

    init { loadWalletsAndAll() }

    private fun loadWalletsAndAll() {
        viewModelScope.launch {
            walletRepository.getWallets().onSuccess { list ->
                _wallets.value = list
                _selectedWalletId.value = DashboardViewModel.defaultWallet(list)
            }
            _walletsLoading.value = false
            categoryRepository.getCategories()
                .onSuccess { _categories.value = it.filter { c -> c.type != "income" } }
            loadBudgets()
        }
    }

    fun setWallet(walletId: String) {
        _selectedWalletId.value = walletId
        loadBudgets()
    }

    fun loadAll() = loadBudgets()

    private fun loadBudgets() {
        viewModelScope.launch {
            _budgets.value = UiState.Loading
            budgetRepository.getBudgets(walletId = _selectedWalletId.value)
                .onSuccess { _budgets.value = UiState.Success(it) }
                .onFailure { _budgets.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun createBudget(categoryId: String, amount: Double) {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val now = LocalDate.now()
        val startDate = now.withDayOfMonth(1).format(fmt)
        val endDate   = now.withDayOfMonth(now.lengthOfMonth()).format(fmt)
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            budgetRepository.createBudget(categoryId, amount, startDate, endDate, _selectedWalletId.value)
                .onSuccess { _saveState.value = UiState.Success(Unit); loadBudgets() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun updateBudget(id: String, amount: Double) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            budgetRepository.updateBudget(id, amount)
                .onSuccess { _saveState.value = UiState.Success(Unit); loadBudgets() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id).onSuccess { loadBudgets() }
        }
    }

    fun resetSaveState() { _saveState.value = UiState.Idle }
}
