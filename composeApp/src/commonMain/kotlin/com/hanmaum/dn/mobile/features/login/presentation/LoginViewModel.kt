package com.hanmaum.dn.mobile.features.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.invalidateBearerCache
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Wir instanziieren das Repo hier direkt (später nutzen wir DI wie Koin)
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val tokenStorage: TokenStorage,
    private val httpClient: HttpClient,
) : ViewModel() {

    // 1. UI State: Single Source of Truth
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // 2. Events verarbeiten
    fun onLoginClicked(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy( error = "아이디와 비밀번호를 입력해주세요.") }
        }

        viewModelScope.launch {
            // State Update: Ladebalken an
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = "인증하는 중입니다. 잠시만 기다려주세요.") }


            try {
                // UseCase ausführen
                val tokenResponse = authRepository.login(user, pass)

                // TOKEN Speichern
                tokenStorage.saveAccessToken(tokenResponse.accessToken)
                tokenStorage.saveRefreshToken(tokenResponse.refreshToken)

                // Force Ktor's BearerAuthProvider to drop any cached (possibly
                // stale) tokens so the very next authed call reads the ones we
                // just saved.
                httpClient.invalidateBearerCache()

                _uiState.update { it.copy(statusMessage = "사용자 정보를 확인 중입니다. 잠시만 기다려주세요.") }

                // PROFIL & STATUS CHECK (/me)
                val profileResult = memberRepository.getMyProfile()

                profileResult.onSuccess { member ->
                    // DECIDE
                    if (member.status == MemberStatus.ACTIVE) {
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
                    tokenStorage.clear()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "사용자 정보를 가져오지 못했습니다. 로그인을 다시 시도해주세요.",
                            statusMessage = "로그인 실패하였습니다."
                        )
                    }
                }

            } catch (e: Exception) {
                // login failed
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "로그인에 실패했습니다. 아이디나 비밀번호를 확인해주세요.",
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