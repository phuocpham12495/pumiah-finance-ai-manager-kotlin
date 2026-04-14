package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.Category
import com.phuocpham.pumiah.data.model.Transaction
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.model.Wallet
import com.phuocpham.pumiah.data.repository.AuthRepository
import com.phuocpham.pumiah.data.repository.CategoryRepository
import com.phuocpham.pumiah.data.repository.TransactionRepository
import com.phuocpham.pumiah.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _transactions = MutableStateFlow<UiState<List<Transaction>>>(UiState.Idle)
    val transactions: StateFlow<UiState<List<Transaction>>> = _transactions

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

    // Week-based filter: 0 = current week, -1 = previous, +1 = next. Week starts Sunday.
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    fun shiftWeek(delta: Int) { _weekOffset.value += delta }
    fun resetWeek() { _weekOffset.value = 0 }

    val currentUserId: String? get() = authRepository.currentUser?.id

    init { loadWalletsAndAll() }

    private fun loadWalletsAndAll() {
        viewModelScope.launch {
            // Load wallets first, then default-select most recently created
            walletRepository.getWallets().onSuccess { list ->
                _wallets.value = list
                _selectedWalletId.value = DashboardViewModel.defaultWallet(list)
            }
            _walletsLoading.value = false
            categoryRepository.getCategories().onSuccess { _categories.value = it }
            loadTransactions()
        }
    }

    fun setWallet(walletId: String) {
        _selectedWalletId.value = walletId
        loadTransactions()
    }

    fun loadAll() = loadTransactions()

    private fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = UiState.Loading
            transactionRepository.getTransactions(walletId = _selectedWalletId.value)
                .onSuccess { _transactions.value = UiState.Success(it) }
                .onFailure { _transactions.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun createTransaction(
        categoryId: String, amount: Double, type: String,
        date: String, notes: String?, walletId: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            transactionRepository.createTransaction(categoryId, amount, type, date, notes, walletId)
                .onSuccess { _saveState.value = UiState.Success(Unit); loadTransactions() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun updateTransaction(
        id: String, categoryId: String, amount: Double, type: String,
        date: String, notes: String?, walletId: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            transactionRepository.updateTransaction(id, categoryId, amount, type, date, notes, walletId)
                .onSuccess { _saveState.value = UiState.Success(Unit); loadTransactions() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id).onSuccess { loadTransactions() }
        }
    }

    fun resetSaveState() { _saveState.value = UiState.Idle }
}
