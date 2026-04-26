package com.hanmaum.dn.mobile.features.pending.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val tokenStorage: TokenStorage,
    private val memberRepository: MemberRepository,
    private val geofenceCoordinator: GeofenceCoordinator,
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
                        else -> handleAuthError()
                    }
                }
                .onFailure { error ->
                    println("Auto-Login fehlgeschlagen: ${error.message}")
                    handleAuthError()
                }
        }
    }

    private fun handleAuthError() {
        viewModelScope.launch {
            tokenStorage.clear()
            _navigateTo.value = NavRoute.Login
        }
    }

    fun onNavigationHandled() {
        _navigateTo.value = null
    }
}
