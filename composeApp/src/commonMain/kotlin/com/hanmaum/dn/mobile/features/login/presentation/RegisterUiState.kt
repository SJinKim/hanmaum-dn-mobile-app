package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.domain.model.NavRoute

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val navigateTo: NavRoute? = null,
    val error: String? = null,

    // Formular Felder
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val baptism: String = "",
    val city: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val phoneNumber: String = "",
    val street: String = "",
    val zipCode: String = ""
)
