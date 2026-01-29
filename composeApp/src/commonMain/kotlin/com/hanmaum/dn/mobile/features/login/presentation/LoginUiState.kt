package com.hanmaum.dn.mobile.features.login.presentation

data class LoginUiState(
    val isLoading: Boolean = false,
    val token: String? = null,
    val error: String? = null,
    val statusMessage: String = "Bitte einloggen"
)