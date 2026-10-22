package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.i18n.UiMessage
import com.hanmaum.dn.mobile.core.domain.model.NavRoute

data class LoginUiState(
    val isLoading: Boolean = false,
    val browserOpen: Boolean = false,
    val biometricExpired: Boolean = false,
    val token: String? = null,
    val error: UiMessage? = null,
    val statusMessage: UiMessage? = null,
    val isSuccess: Boolean = false,
    val navigateTo: NavRoute? = null
)
