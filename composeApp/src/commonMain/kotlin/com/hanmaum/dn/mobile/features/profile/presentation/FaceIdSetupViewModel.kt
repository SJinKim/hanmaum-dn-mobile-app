package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.lifecycle.ViewModel
import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.BiometricAvailability
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.core.security.VaultResult
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FaceIdSetupUiState(
    val enabled: Boolean = false,
    val isBusy: Boolean = false,
    val error: String? = null,
)

/**
 * Switching Face ID sign-in on and off.
 *
 * Enabling costs one biometric prompt and no password: the member is already
 * signed in, so the refresh token that Face ID will replay is right there. It
 * is sealed into the [BiometricVault], where the OS — not this code — decides
 * whether to hand it back (#200).
 *
 * The vault is passed in per call rather than injected: it needs the hosting
 * activity on Android, so it can only be built inside composition.
 */
class FaceIdSetupViewModel(
    private val tokenStorage: TokenStorage,
    private val authPreferences: AuthPreferences,
    /**
     * Asked who is signed in at the moment of sealing. The arming records its
     * owner so another member's password sign-in can clear it (#221), and the
     * owner has to come from the session being sealed — not from a value only
     * the password form ever wrote, which an auto-login left empty or stale.
     */
    private val memberRepository: MemberRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceIdSetupUiState(enabled = authPreferences.isBiometricEnabled()))
    val uiState = _uiState.asStateFlow()

    /** Re-read the stored flag — turning "keep me signed in" off also clears it. */
    fun refresh() {
        _uiState.update { it.copy(enabled = authPreferences.isBiometricEnabled()) }
    }

    suspend fun enable(vault: BiometricVault, title: String, subtitle: String, cancelLabel: String) {
        val refreshToken = tokenStorage.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            // Nothing to seal. Rather than arm a switch that would fail at the
            // next launch, say so now.
            _uiState.update { it.copy(error = "다시 로그인한 뒤 설정해주세요.") }
            return
        }

        _uiState.update { it.copy(isBusy = true, error = null) }

        // Before the prompt, not after it: a member who has just looked at the
        // camera should not then be told the setup failed on a network call.
        val owner = memberRepository.getMyProfile().getOrNull()?.publicId
        if (owner == null) {
            _uiState.update { it.copy(isBusy = false, error = "사용자 정보를 확인하지 못했습니다. 다시 시도해주세요.") }
            return
        }

        val result = vault.seal(refreshToken, title, subtitle, cancelLabel)
        // Read right after the refusal, so the message names the actual reason.
        val unavailable = if (result == VaultResult.Unavailable) vault.availability() else null
        _uiState.update { state ->
            when (result) {
                is VaultResult.Success -> {
                    authPreferences.setBiometricEnabled(true)
                    authPreferences.setBiometricMemberId(owner)
                    state.copy(enabled = true, isBusy = false)
                }
                // Backing out is a choice, not an error worth a message.
                VaultResult.Cancelled -> state.copy(isBusy = false)
                VaultResult.Unavailable -> state.copy(
                    isBusy = false,
                    error = when (unavailable) {
                        BiometricAvailability.NOT_ENROLLED -> "기기에 등록된 생체 인증이 없습니다."
                        BiometricAvailability.DENIED -> "iPhone 설정에서 이 앱의 Face ID 사용을 허용해주세요."
                        else -> "지금은 생체 인증을 사용할 수 없습니다. 잠시 후 다시 시도해주세요."
                    },
                )
                else -> state.copy(isBusy = false, error = "설정에 실패했습니다. 다시 시도해주세요.")
            }
        }
    }

    fun disable(vault: BiometricVault) {
        authPreferences.setBiometricEnabled(false)
        vault.clear()
        _uiState.update { it.copy(enabled = false, error = null) }
    }
}
