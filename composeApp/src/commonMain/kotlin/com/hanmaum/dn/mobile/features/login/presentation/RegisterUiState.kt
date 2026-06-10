package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.features.login.domain.model.PasswordCriteria

/** Language-independent validation error per field; resolved to text in the UI. */
enum class RegisterFieldError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_REQUIREMENTS,
    DATE_INCOMPLETE,
    DATE_INVALID,
    INVALID_POSTCODE,
    INVALID_CITY,
    INVALID_HOUSE_NUMBER,
}

/** Identifies a field so the UI can focus/scroll to the first invalid one. */
enum class RegisterField {
    FIRST_NAME,
    LAST_NAME,
    EMAIL,
    CITY,
    ZIP_CODE,
    PASSWORD,
    STREET,
    HOUSE_NUMBER,
}

/** Top-of-form banner — distinct from per-field errors. */
sealed interface RegisterBanner {
    /** A clean message provided by the backend (e.g. "email already exists"). */
    data class ServerMessage(val text: String) : RegisterBanner
    /** Unexpected/network failure — UI shows a localized generic message. */
    data object Generic : RegisterBanner
    /** Registered, but auto-login failed — UI prompts the user to log in. */
    data object RegisteredPleaseLogin : RegisterBanner
}

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val navigateTo: NavRoute? = null,
    val bannerError: RegisterBanner? = null,

    // Formular Felder
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val passwordCriteria: PasswordCriteria = PasswordCriteria(),
    val baptism: String = "",
    val city: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val phoneNumber: String = "",      // national digits only; combined with dial code on submit
    val phoneCountryIso: String = "DE", // selected country for the dial-code prefix
    val street: String = "",       // street name only — combined with houseNumber on submit
    val houseNumber: String = "",  // German "Hausnummer" — combined with street on submit
    val zipCode: String = "",

    // Per-field validation errors
    val firstNameError: RegisterFieldError? = null,
    val lastNameError: RegisterFieldError? = null,
    val emailError: RegisterFieldError? = null,
    val cityError: RegisterFieldError? = null,
    val zipCodeError: RegisterFieldError? = null,
    val passwordError: RegisterFieldError? = null,
    val birthDateError: RegisterFieldError? = null,
    val streetError: RegisterFieldError? = null,
    val houseNumberError: RegisterFieldError? = null,

    // Field the UI should focus/scroll to after a failed submit
    val focusTarget: RegisterField? = null,
)
