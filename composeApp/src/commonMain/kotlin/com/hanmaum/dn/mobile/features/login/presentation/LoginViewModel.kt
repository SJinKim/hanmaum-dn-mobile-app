package com.hanmaum.dn.mobile.features.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.invalidateBearerCache
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.core.security.VaultResult
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
    private val authPreferences: AuthPreferences,
    /**
     * Only ever cleared through this one, never opened: clearing needs no prompt
     * and no activity, which is why the injected instance is enough here while
     * the screens pass their own composition-bound vault into the calls below.
     */
    private val biometricVault: BiometricVault,
) : ViewModel() {

    // 1. UI State: Single Source of Truth
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    /** True when Face ID sign-in is armed: switched on and a secret is sealed. */
    fun canFaceIdSignIn(vault: BiometricVault): Boolean =
        authPreferences.isBiometricEnabled() && vault.hasSecret()

    /**
     * Signs in with Face ID.
     *
     * The vault releases the refresh token only against a real biometric match,
     * and that token is then traded for a session. Nothing here replays a
     * password — there is none stored to replay (#200).
     */
    suspend fun signInWithFaceId(
        vault: BiometricVault,
        title: String,
        subtitle: String,
        cancelLabel: String,
    ) {
        when (val opened = vault.open(title, subtitle, cancelLabel)) {
            is VaultResult.Success -> exchangeRefreshToken(vault, opened.value)
            // Dismissing the prompt is a choice: fall back to the form quietly.
            VaultResult.Cancelled -> Unit
            VaultResult.Invalidated -> {
                // Biometrics were re-enrolled; the secret is gone for good.
                authPreferences.setBiometricEnabled(false)
                _uiState.update { it.copy(error = "생체 인증이 변경되어 다시 설정해야 합니다.") }
            }
            // Nothing sealed. The item is gone for good, so the switch follows it.
            VaultResult.Empty -> authPreferences.setBiometricEnabled(false)
            // "Not available right now" — a biometric lockout after failed attempts
            // reports exactly this, on both platforms. Switching the setting off
            // here made a passing condition permanent: the member had to set Face ID
            // up again to get the button back (#212).
            VaultResult.Unavailable -> Unit
            VaultResult.Failed ->
                _uiState.update { it.copy(error = "생체 인증에 실패했습니다. 비밀번호로 로그인해주세요.") }
        }
    }

    private suspend fun exchangeRefreshToken(vault: BiometricVault, refreshToken: String) {
        _uiState.update { it.copy(isLoading = true, error = null, statusMessage = "인증하는 중입니다. 잠시만 기다려주세요.") }
        try {
            val tokens = authRepository.refresh(refreshToken)
            tokenStorage.saveAccessToken(tokens.accessToken)
            tokens.refreshToken?.let {
                tokenStorage.saveRefreshToken(it)
                // The token that just bought this session is spent. Without writing
                // the rotated one back, the vault keeps a dead token and Face ID
                // works exactly once (#212). The prompt from open() still counts,
                // so this costs the member nothing.
                vault.reseal(it)
            }
            httpClient.invalidateBearerCache()
            routeByStatus()
        } catch (e: Exception) {
            // Keycloak's idle timeout has passed, so the sealed token is spent.
            // The password form is the way back in; option C in #200 is what
            // would avoid this, and it needs the server.
            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "",
                    error = "다시 로그인해주세요.",
                )
            }
        }
    }

    // 2. Events verarbeiten
    /**
     * Face ID is not armed from here. It is switched on in 설정, where the member
     * is already signed in and the refresh token is there to seal — this screen
     * carried an `enableFaceId` flag that nothing ever read (#212).
     */
    fun onLoginClicked(user: String, pass: String, keepSignedIn: Boolean = true) {
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
                // Remember the user's "Keep me signed in" choice so the splash
                // screen knows whether to auto-login on the next app launch.
                authPreferences.setKeepSignedInEnabled(keepSignedIn)

                // Force Ktor's BearerAuthProvider to drop any cached (possibly
                // stale) tokens so the very next authed call reads the ones we
                // just saved.
                httpClient.invalidateBearerCache()

                routeByStatus()

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

    /**
     * Fetches the profile and routes by member status.
     *
     * Shared by the password form and Face ID: both end with a session in hand
     * and the same question — where does this member belong?
     */
    private suspend fun routeByStatus() {
        _uiState.update { it.copy(statusMessage = "사용자 정보를 확인 중입니다. 잠시만 기다려주세요.") }

        memberRepository.getMyProfile()
            .onSuccess { member ->
                // The Face ID arming outlives a sign-out (#218), so a second
                // member on the same device would otherwise meet a button that
                // hands them the first member's session. Their face would not
                // open it, but the button has no business being there.
                val armedFor = authPreferences.biometricMemberId()
                if (armedFor != null && armedFor != member.publicId) {
                    authPreferences.setBiometricEnabled(false)
                    biometricVault.clear()
                }
                authPreferences.setSignedInMemberId(member.publicId)

                // Sending every non-active member to the pending screen used to
                // tell a refused applicant to wait for an approval that was
                // never coming.
                val destination = when (member.status) {
                    MemberStatus.ACTIVE -> NavRoute.Home
                    MemberStatus.REJECTED -> NavRoute.Rejected
                    // INACTIVE / DELETED / UNKNOWN still land here. That is not
                    // right either — they are not waiting for anything — but what
                    // they should see is its own question, and guessing at it
                    // would repeat the mistake this change is fixing.
                    else -> NavRoute.PendingApproval
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = if (destination == NavRoute.Home) "인증 완료!" else it.statusMessage,
                        isSuccess = true,
                        navigateTo = destination,
                    )
                }
            }
            .onFailure {
                tokenStorage.clear()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "사용자 정보를 가져오지 못했습니다. 로그인을 다시 시도해주세요.",
                        statusMessage = "로그인 실패하였습니다.",
                    )
                }
            }
    }

    // Nach Navigation State resetten, damit er nicht immer wieder navigiert
    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateTo = null) }
    }
}