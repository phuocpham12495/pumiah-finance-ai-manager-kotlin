package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.model.UserProfile
import com.phuocpham.pumiah.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UiState<UserProfile>>(UiState.Idle)
    val profile: StateFlow<UiState<UserProfile>> = _profile

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateState: StateFlow<UiState<Unit>> = _updateState

    private val _avatarUploadState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val avatarUploadState: StateFlow<UiState<String>> = _avatarUploadState

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _profile.value = UiState.Loading
            profileRepository.getProfile()
                .onSuccess { _profile.value = UiState.Success(it) }
                .onFailure { _profile.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun updateProfile(dateOfBirth: String?) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            profileRepository.updateProfile(dateOfBirth)
                .onSuccess { _updateState.value = UiState.Success(Unit); loadProfile() }
                .onFailure { _updateState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, extension: String) {
        viewModelScope.launch {
            _avatarUploadState.value = UiState.Loading
            profileRepository.uploadAvatar(imageBytes, extension)
                .onSuccess { url ->
                    _avatarUploadState.value = UiState.Success(url)
                    loadProfile()
                }
                .onFailure { _avatarUploadState.value = UiState.Error(it.message ?: "Không thể tải ảnh lên") }
        }
    }

    fun resetUpdateState() { _updateState.value = UiState.Idle }
    fun resetAvatarUploadState() { _avatarUploadState.value = UiState.Idle }
}
