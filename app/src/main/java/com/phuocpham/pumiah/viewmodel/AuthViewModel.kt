package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val authState: StateFlow<UiState<Unit>> = _authState

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn()

    init {
        viewModelScope.launch {
            authRepository.awaitSessionReady()
            _sessionReady.value = true
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            authRepository.signIn(email, password)
                .onSuccess { _authState.value = UiState.Success(Unit) }
                .onFailure { _authState.value = UiState.Error(friendlyError(it.message, isLogin = true)) }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            authRepository.signUp(email, password)
                .onSuccess { _authState.value = UiState.Success(Unit) }
                .onFailure { _authState.value = UiState.Error(friendlyError(it.message, isLogin = false)) }
        }
    }

    private fun friendlyError(message: String?, isLogin: Boolean): String {
        val msg = message?.lowercase() ?: ""
        return when {
            "email not confirmed" in msg || "email_not_confirmed" in msg ->
                "Email chưa được xác nhận. Vui lòng kiểm tra hộp thư và xác nhận tài khoản."
            "invalid login credentials" in msg || "invalid_credentials" in msg ->
                "Email hoặc mật khẩu không đúng."
            "user already registered" in msg || "already registered" in msg ->
                "Email này đã được đăng ký. Vui lòng đăng nhập."
            "password should be at least" in msg ->
                "Mật khẩu phải có ít nhất 6 ký tự."
            "unable to validate email" in msg || "invalid email" in msg ->
                "Địa chỉ email không hợp lệ."
            "network" in msg || "connect" in msg || "timeout" in msg ->
                "Không có kết nối mạng. Vui lòng thử lại."
            "rate limit" in msg || "too many" in msg ->
                "Quá nhiều lần thử. Vui lòng đợi vài phút rồi thử lại."
            else -> if (isLogin) "Đăng nhập thất bại. Vui lòng thử lại." else "Đăng ký thất bại. Vui lòng thử lại."
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onDone()
        }
    }

    private val _resetPasswordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val resetPasswordState: StateFlow<UiState<Unit>> = _resetPasswordState

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = UiState.Loading
            authRepository.resetPassword(email)
                .onSuccess { _resetPasswordState.value = UiState.Success(Unit) }
                .onFailure { _resetPasswordState.value = UiState.Error(friendlyError(it.message, isLogin = false)) }
        }
    }

    fun resetState() { _authState.value = UiState.Idle }
    fun resetPasswordStateReset() { _resetPasswordState.value = UiState.Idle }
}
