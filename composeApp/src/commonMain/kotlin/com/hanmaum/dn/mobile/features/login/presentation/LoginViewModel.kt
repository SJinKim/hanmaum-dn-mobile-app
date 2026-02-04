package com.hanmaum.dn.mobile.features.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.NetworkClient
import com.hanmaum.dn.mobile.features.login.data.repository.AuthRepositoryImpl
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImpl
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Wir instanziieren das Repo hier direkt (später nutzen wir DI wie Koin)
class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val memberRepository: MemberRepository = MemberRepositoryImpl(NetworkClient.client),
    private val tokenStorage: TokenStorage = NetworkClient.tokenStorage
) : ViewModel() {

    // 1. UI State: Single Source of Truth
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // 2. Events verarbeiten
    fun onLoginClicked(user: String, pass: String) {
        viewModelScope.launch {
            // State Update: Ladebalken an
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                statusMessage = "인증하는중입니다..."
            )

            try {
                // UseCase ausführen
                val tokenResponse = authRepository.login(user, pass)

                // TOKEN Speichern
                tokenStorage.saveAccessToken(tokenResponse.accessToken)
                tokenStorage.saveRefreshToken(tokenResponse.refreshToken)

                _uiState.update { it.copy(statusMessage = "로딩중입니다") }

                // PROFIL & STATUS CHECK (/me)
                val profileResult = memberRepository.getMyProfile()

                profileResult.onSuccess { member ->
                    // DECIDE
                    if (member.status == "ACTIVE") {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                statusMessage = "인증 완료!",
                                isSuccess = true,
                                navigateTo = NavRoute.Home
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                navigateTo = NavRoute.PendingApproval
                            )
                        }
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Profil-Fehler: ${e.message}",
                            statusMessage = "로그인 실패!"
                        )
                    }
                }

            } catch (e: Exception) {
                // login failed
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Login fehlgeschlagen. Bitte Daten prüfen.",
                        statusMessage = ""
                    )
                }
            }
        }
    }

    // Nach Navigation State resetten, damit er nicht immer wieder navigiert
    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateTo = null) }
    }
}