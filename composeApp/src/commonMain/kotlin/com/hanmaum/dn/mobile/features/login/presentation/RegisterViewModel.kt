package com.hanmaum.dn.mobile.features.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    // --- Events zum Ändern der Felder ---
    fun onFirstNameChange(v: String) = _uiState.update { it.copy(firstName = v, error = null) }
    fun onLastNameChange(v: String) = _uiState.update { it.copy(lastName = v, error = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onCityChange(v: String) = _uiState.update { it.copy(city = v, error = null) }

    // Optionale
    fun onBaptismChange(v: String) = _uiState.update { it.copy(baptism = v) }
    fun onGenderChange(v: String) = _uiState.update { it.copy(gender = v) }
    fun onBirthDateChange(v: String) = _uiState.update { it.copy(birthDate = v) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phoneNumber = v) }
    fun onStreetChange(v: String) = _uiState.update { it.copy(street = v) }
    fun onZipChange(v: String) = _uiState.update { it.copy(zipCode = v) }

    fun register() {
        val s = _uiState.value

        // 1. VALIDIERUNG
        // Nur noch Name, Email und Stadt sind harte Pflichtfelder
        if (s.firstName.isBlank() || s.lastName.isBlank() || s.email.isBlank() || s.password.isBlank() || s.zipCode.isBlank() || s.city.isBlank() ) {
            _uiState.update { it.copy(error = "필수 항목입니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 2. MAPPING (String -> Nullable für Backend)
            val request = RegisterRequest(
                firstName = s.firstName,
                lastName = s.lastName,
                email = s.email,
                password = s.password,
                city = s.city,
                baptism = s.baptism.ifBlank { null },
                gender = s.gender.ifBlank { null },
                birthDate = s.birthDate.ifBlank { null },
                phoneNumber = s.phoneNumber.ifBlank { null },
                street = s.street.ifBlank { null },
                zipCode = s.zipCode.ifBlank { null }
            )

            // 1. REGISTRIERUNG AUFRUFEN
            val result = authRepository.register(request)

            result.onSuccess {
                // --> Erfolg! Jetzt Auto-Login
                performAutoLogin(s.email, s.password)
            }.onFailure { exception ->
                // --> Fehler! Zeige die Nachricht vom Backend an (z.B. "Email existiert schon")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Ein Fehler ist aufgetreten."
                    )
                }
            }
        }
    }

    private fun performAutoLogin(email: String, pass: String) {
        viewModelScope.launch {
            try {
                // Dein existierender Login Code
                val tokenResponse = authRepository.login(email, pass)

                tokenStorage.saveAccessToken(tokenResponse.accessToken)
                tokenStorage.saveRefreshToken(tokenResponse.refreshToken)

                // Login erfolgreich -> Success State setzen
                // (Token müsste eigentlich gespeichert werden, hier vereinfacht)
                _uiState.update { it.copy(isLoading = false, isSuccess = true, navigateTo = NavRoute.PendingApproval) }

            } catch (e: Exception) {
                // Registriert ja, aber Login fehlgeschlagen
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = "등록 성공했습니다. 로그인 해주세요."
                    )
                }
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateTo = null) }
    }

}