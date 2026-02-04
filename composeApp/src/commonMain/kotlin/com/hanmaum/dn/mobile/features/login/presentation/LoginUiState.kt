package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.domain.model.NavRoute

data class LoginUiState(
    val isLoading: Boolean = false,
    val token: String? = null,
    val error: String? = null,
    val statusMessage: String = "Bitte einloggen",
    val isSuccess: Boolean = false,
    val navigateTo: NavRoute? = null
)