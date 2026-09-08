package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecords
import com.hanmaum.dn.mobile.features.verse.domain.repository.VerseRecordRepository
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val tokenStorage: TokenStorage,
    private val biometricVault: BiometricVault,
    private val notificationRepository: NotificationRepository,
    private val pushManager: PushManager,
    private val authPreferences: AuthPreferences,
    private val verseRecordRepository: VerseRecordRepository,
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    /**
     * The two 말씀 streaks, kept beside [uiState] rather than inside it.
     *
     * They are orthogonal to loading the profile: the tiles show a dash until
     * they arrive, and a failed streak call must not turn the whole screen into
     * an error. Folding them into the sealed state would tie the two together.
     */
    private val _verseRecords = MutableStateFlow<VerseRecords?>(null)
    val verseRecords: StateFlow<VerseRecords?> = _verseRecords.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Loads (or reloads) the profile. Driven by the screen on every entry so a
     * profile edited elsewhere (e.g. the web app) stays current without a
     * re-login. Refreshes silently when data is already shown, skips entirely
     * while an edit is in progress (so it is never clobbered), and keeps the
     * current data on a transient refresh failure.
     */
    private fun loadVerseRecords() {
        viewModelScope.launch {
            verseRecordRepository.getRecords()
                .onSuccess { _verseRecords.value = it }
            // onFailure: the tiles keep their dash.
        }
    }

    fun loadProfile() {
        loadVerseRecords()
        viewModelScope.launch {
            val current = _uiState.value
            if (current is ProfileUiState.Success && current.isDirty) return@launch
            val hadData = current is ProfileUiState.Success
            if (!hadData) _uiState.value = ProfileUiState.Loading
            memberRepository.getMyProfile().fold(
                onSuccess = { _uiState.value = ProfileUiState.Success(it) },
                onFailure = { if (!hadData) _uiState.value = ProfileUiState.Error(it.message ?: "프로필 로딩 실패") },
            )
        }
    }

    fun startEditing() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(isEditing = true, saveError = null)
    }

    /**
     * Leaves edit mode and throws away the draft.
     *
     * The edit fields are reset from the loaded profile rather than merely
     * hidden — otherwise re-entering edit mode would show the abandoned draft
     * as if it had been saved.
     */
    fun cancelEditing() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = ProfileUiState.Success(profile = current.profile)
    }

    fun updateBirthDate(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editBirthDate = value)
    }

    fun consumeSaveSuccess() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (current.saveSuccess) _uiState.value = current.copy(saveSuccess = false)
    }

    fun updatePhone(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editPhone = value)
    }

    fun updateImageUrl(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editImageUrl = value)
    }

    fun updateStreet(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editStreet = value)
    }

    fun updateHouseNumber(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editHouseNumber = value)
    }

    fun updateZipCode(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editZipCode = value)
    }

    fun updateCity(value: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(editCity = value)
    }

    fun logout() {
        viewModelScope.launch {
            // Best-effort: stop push to this device for the signed-out account.
            // Must run before the token clear or the call goes out unauthenticated.
            pushManager.currentToken()?.let { notificationRepository.deleteDeviceToken(it) }
            tokenStorage.clear()
            // Explicit logout is an intentional teardown: drop the sealed
            // refresh token AND the flag so the next sign-in starts clean.
            // (A plain session expiry keeps both, so Face ID still works — see
            // TokenStorageImpl.clear.)
            authPreferences.setBiometricEnabled(false)
            biometricVault.clear()
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
                birthDate = current.editBirthDate.replace('.', '-').takeIf { it.length == 10 },
                street = current.editStreet.ifBlank { null },
                houseNumber = current.editHouseNumber.ifBlank { null },
                zipCode = current.editZipCode.ifBlank { null },
                city = current.editCity.ifBlank { null },
            ).fold(
                onSuccess = { updated -> _uiState.value = ProfileUiState.Success(updated).copy(saveSuccess = true) },
                onFailure = { err ->
                    _uiState.value = current.copy(isSaving = false, saveError = err.message ?: "저장 실패")
                },
            )
        }
    }
}
