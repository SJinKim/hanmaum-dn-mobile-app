package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.features.login.domain.model.LoginException
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaceIdSetupUiState(
    val enabled: Boolean = false,
    val askingForPassword: Boolean = false,
    val password: String = "",
    val isVerifying: Boolean = false,
    val error: String? = null,
)

/**
 * Turning Face ID sign-in on, including the password it needs.
 *
 * The switch alone cannot arm anything: Face ID replays a stored password, and
 * this screen is reachable only while already signed in, so there is no
 * password around to store. Before the prompt the toggle therefore did nothing
 * until the member happened to sign in by hand again — which, with "keep me
 * signed in" on, may never happen (#197).
 *
 * The typed password is verified against Keycloak before it is stored. Storing
 * it unverified would move the failure to the next launch, where it surfaces as
 * "Face ID is broken" rather than "that was the wrong password".
 */
class FaceIdSetupViewModel(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val credentialStore: CredentialStore,
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceIdSetupUiState(enabled = authPreferences.isBiometricEnabled()))
    val uiState = _uiState.asStateFlow()

    /** Re-read the stored flag — turning "keep me signed in" off also clears it. */
    fun refresh() {
        _uiState.update { it.copy(enabled = authPreferences.isBiometricEnabled()) }
    }

    fun onToggle(want: Boolean) {
        if (want) {
            _uiState.update { it.copy(askingForPassword = true, password = "", error = null) }
        } else {
            authPreferences.setBiometricEnabled(false)
            credentialStore.clear()
            _uiState.update { it.copy(enabled = false, askingForPassword = false, password = "", error = null) }
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onDismiss() {
        // Backing out leaves the switch where it was: nothing was stored.
        _uiState.update { it.copy(askingForPassword = false, password = "", error = null, isVerifying = false) }
    }

    fun onConfirm() {
        val password = _uiState.value.password
        if (password.isBlank()) {
            _uiState.update { it.copy(error = "비밀번호를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, error = null) }

            val email = memberRepository.getMyProfile().getOrNull()?.email
            if (email.isNullOrBlank()) {
                // Face ID signs in with an email and a password. Without the
                // address there is nothing to store that would ever work.
                _uiState.update {
                    it.copy(isVerifying = false, error = "계정 정보를 불러오지 못했습니다. 다시 시도해주세요.")
                }
                return@launch
            }

            try {
                authRepository.login(email, password)
                credentialStore.saveCredentials(email, password)
                authPreferences.setBiometricEnabled(true)
                _uiState.update {
                    it.copy(enabled = true, askingForPassword = false, password = "", isVerifying = false)
                }
            } catch (e: LoginException) {
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        error = if (e.isAccountNotFullySetUp) {
                            "이메일 확인이 아직 완료되지 않았습니다."
                        } else {
                            "비밀번호가 올바르지 않습니다."
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isVerifying = false, error = "연결에 실패했습니다. 다시 시도해주세요.") }
            }
        }
    }
}
