package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.model.Wallet
import com.phuocpham.pumiah.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _wallets = MutableStateFlow<UiState<List<Wallet>>>(UiState.Idle)
    val wallets: StateFlow<UiState<List<Wallet>>> = _wallets

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState

    init { loadWallets() }

    fun loadWallets() {
        viewModelScope.launch {
            _wallets.value = UiState.Loading
            walletRepository.getWallets()
                .onSuccess { _wallets.value = UiState.Success(it) }
                .onFailure { _wallets.value = UiState.Error(it.message ?: "Lỗi tải ví") }
        }
    }

    fun createWallet(name: String, type: String = "personal") {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            walletRepository.createWallet(name, type)
                .onSuccess { _actionState.value = UiState.Success(Unit); loadWallets() }
                .onFailure { _actionState.value = UiState.Error(it.message ?: "Lỗi tạo ví") }
        }
    }

    fun inviteParticipant(walletId: String, email: String, role: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            walletRepository.inviteParticipant(walletId, email, role)
                .onSuccess { _actionState.value = UiState.Success(Unit); loadWallets() }
                .onFailure { _actionState.value = UiState.Error(it.message ?: "Không tìm thấy email hoặc lỗi mời") }
        }
    }

    fun removeParticipant(walletId: String, userId: String) {
        viewModelScope.launch {
            walletRepository.removeParticipant(walletId, userId)
                .onSuccess { loadWallets() }
                .onFailure { _actionState.value = UiState.Error(it.message ?: "Lỗi xoá thành viên") }
        }
    }

    fun updateRole(walletId: String, userId: String, newRole: String) {
        viewModelScope.launch {
            walletRepository.updateParticipantRole(walletId, userId, newRole)
                .onSuccess { loadWallets() }
                .onFailure { _actionState.value = UiState.Error(it.message ?: "Lỗi cập nhật vai trò") }
        }
    }

    fun deleteWallet(walletId: String) {
        viewModelScope.launch {
            walletRepository.deleteWallet(walletId)
                .onSuccess { loadWallets() }
                .onFailure { _actionState.value = UiState.Error(it.message ?: "Lỗi xoá ví") }
        }
    }

    fun leaveWallet(walletId: String) {
        viewModelScope.launch {
            walletRepository.leaveWallet(walletId)
                .onSuccess { loadWallets() }
                .onFailure { _actionState.value = UiState.Error(it.message ?: "Lỗi rời ví") }
        }
    }

    fun resetActionState() { _actionState.value = UiState.Idle }
}
