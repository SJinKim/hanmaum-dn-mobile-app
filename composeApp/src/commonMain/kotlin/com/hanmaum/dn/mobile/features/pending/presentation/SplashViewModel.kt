package com.hanmaum.dn.mobile.features.pending.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val tokenStorage: TokenStorage,
    private val memberRepository: MemberRepository,
    private val geofenceCoordinator: GeofenceCoordinator,
    private val credentialStore: CredentialStore,
) : ViewModel() {

    private val _navigateTo = MutableStateFlow<NavRoute?>(null)
    val navigateTo = _navigateTo.asStateFlow()

    init { checkSession() }

    private fun checkSession() {
        viewModelScope.launch {
            val token = tokenStorage.getAccessToken()
            if (token.isNullOrBlank()) {
                _navigateTo.value = NavRoute.Login
                return@launch
            }

            // "Keep me signed in" was off at login → this is a fresh launch, so
            // drop the persisted session and require sign-in again.
            if (!tokenStorage.isKeepSignedIn()) {
                tokenStorage.clear()
                _navigateTo.value = NavRoute.Login
                return@launch
            }

            memberRepository.getMyProfile()
                .onSuccess { member ->
                    when (member.status) {
                        MemberStatus.ACTIVE -> {
                            // Fire-and-forget: initialize geofence after confirmed login.
                            // No-op if already registered or permission not yet granted.
                            viewModelScope.launch { geofenceCoordinator.initialize() }
                            _navigateTo.value = NavRoute.Home
                        }
                        MemberStatus.PENDING -> _navigateTo.value = NavRoute.PendingApproval
                        // Refused. The session is deliberately kept: clearing it would
                        // drop the member at the login screen, where they could simply
                        // register again — and they would never see why they were
                        // turned away.
                        MemberStatus.REJECTED -> _navigateTo.value = NavRoute.Rejected
                        // DELETED / INACTIVE / UNKNOWN: the account is no longer valid,
                        // so this is an intentional teardown — forget the saved Face ID
                        // setup too.
                        else -> handleAuthError(forgetBiometric = true)
                    }
                }
                .onFailure { error ->
                    // Transient failure (network / refresh). Drop the session but keep
                    // the Face ID setup so the Login screen can still offer it.
                    println("Auto-Login fehlgeschlagen: ${error.message}")
                    handleAuthError(forgetBiometric = false)
                }
        }
    }

    private fun handleAuthError(forgetBiometric: Boolean) {
        viewModelScope.launch {
            tokenStorage.clear()
            if (forgetBiometric) {
                tokenStorage.setBiometricEnabled(false)
                credentialStore.clear()
            }
            _navigateTo.value = NavRoute.Login
        }
    }

    fun onNavigationHandled() {
        _navigateTo.value = null
    }
}
