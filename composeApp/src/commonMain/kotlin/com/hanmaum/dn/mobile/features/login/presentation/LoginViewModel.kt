package com.hanmaum.dn.mobile.features.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.login.data.repository.AuthRepositoryImpl
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Wir instanziieren das Repo hier direkt (später nutzen wir DI wie Koin)
class LoginViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
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
                statusMessage = "Authentifiziere..."
            )

            try {
                // UseCase ausführen
                val tokenResponse = repository.login(user, pass)
                val token = tokenResponse.accessToken

                val backendStatus = repository.testBackendConnection(token)

                // State Update: Erfolg
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    token = token,
                    statusMessage = "Erfolg! Backend: $backendStatus"
                )
            } catch (e: Exception) {
                // State Update: Fehler
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                    statusMessage = "Fehler: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }
}