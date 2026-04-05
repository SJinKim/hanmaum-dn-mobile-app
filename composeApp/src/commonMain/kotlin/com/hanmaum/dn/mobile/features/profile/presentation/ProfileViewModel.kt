package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            memberRepository.getMyProfile().fold(
                onSuccess = { _uiState.value = ProfileUiState.Success(it) },
                onFailure = { _uiState.value = ProfileUiState.Error(it.message ?: "프로필 로딩 실패") },
            )
        }
    }

    fun startEditing() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(isEditing = true)
    }

    fun cancelEditing() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(
            isEditing = false,
            editPhone = current.profile.phoneNumber ?: "",
            editImageUrl = current.profile.profileImageUrl ?: "",
            saveError = null,
        )
    }

    fun updatePhone(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editPhone = value)
    }

    fun updateImageUrl(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editImageUrl = value)
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clear()
            _loggedOut.value = true
        }
    }

    fun saveProfile() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            memberRepository.updateMyProfile(
                phoneNumber = current.editPhone.ifBlank { null },
                profileImageUrl = current.editImageUrl.ifBlank { null },
            ).fold(
                onSuccess = { updated -> _uiState.value = ProfileUiState.Success(updated) },
                onFailure = { err ->
                    _uiState.value = current.copy(isSaving = false, saveError = err.message ?: "저장 실패")
                },
            )
        }
    }
}
