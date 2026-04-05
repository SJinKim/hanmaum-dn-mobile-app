package com.hanmaum.dn.mobile.features.pending.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val tokenStorage: TokenStorage,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _navigateTo = MutableStateFlow<NavRoute?>(null)
    val navigateTo = _navigateTo.asStateFlow()

    // startet sofort beim Initialisieren des ViewModels
    init {
        checkSession()
    }

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
                        MemberStatus.ACTIVE -> _navigateTo.value = NavRoute.Home
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
        // Tokens löschen, damit der User nicht im Loop hängt
        viewModelScope.launch {
            tokenStorage.clear()
            _navigateTo.value = NavRoute.Login
        }
    }
    // Reset Event nach Navigation
    fun onNavigationHandled() {
        _navigateTo.value = null
    }
}
