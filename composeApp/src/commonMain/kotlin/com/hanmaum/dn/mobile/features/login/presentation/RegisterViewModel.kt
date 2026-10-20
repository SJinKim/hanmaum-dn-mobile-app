package com.hanmaum.dn.mobile.features.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
import com.hanmaum.dn.mobile.features.login.domain.model.BirthDateInput
import com.hanmaum.dn.mobile.features.login.domain.model.IdentityStatus
import com.hanmaum.dn.mobile.features.login.domain.model.Countries
import com.hanmaum.dn.mobile.features.login.domain.model.PasswordPolicy
import com.hanmaum.dn.mobile.features.login.domain.model.PhoneNumber
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterException
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterValidation
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.login.domain.repository.CityLookupRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val cityLookupRepository: CityLookupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    // --- Events zum Ändern der Felder ---
    // Editing a field clears its own error and any top-of-form banner.
    fun onFirstNameChange(v: String) = _uiState.update {
        it.copy(firstName = v, firstNameError = null, bannerError = null)
    }
    fun onLastNameChange(v: String) = _uiState.update {
        it.copy(lastName = v, lastNameError = null, bannerError = null)
    }
    fun onEmailChange(v: String) = _uiState.update {
        // Recompute criteria so the "different from your email" check stays in sync.
        it.copy(
            email = v,
            emailError = null,
            bannerError = null,
            passwordCriteria = PasswordPolicy.evaluate(it.password, v),
        )
    }
    fun onPasswordChange(v: String) = _uiState.update {
        it.copy(
            password = v,
            passwordError = null,
            bannerError = null,
            passwordCriteria = PasswordPolicy.evaluate(v, it.email),
        )
    }
    // Tracks the in-flight PLZ->city lookup so a newer postcode cancels the older one.
    private var cityLookupJob: Job? = null

    fun onCityChange(v: String) = _uiState.update {
        // The user is taking over the city — stop autofilling over their input.
        it.copy(city = v, cityError = null, bannerError = null, cityAutofilled = false)
    }
    fun onZipChange(v: String) {
        _uiState.update { it.copy(zipCode = v, zipCodeError = null, bannerError = null) }
        maybeAutofillCity(v)
    }

    /**
     * On a complete German PLZ, resolves the city via [cityLookupRepository] and
     * fills it in — but only while the city is empty or was itself autofilled, so
     * a manually-typed city is never overwritten. Failures are silent: the user
     * keeps typing the city themselves.
     */
    private fun maybeAutofillCity(zip: String) {
        cityLookupJob?.cancel()
        val plz = zip.trim()
        val current = _uiState.value
        val mayAutofill = current.city.isBlank() || current.cityAutofilled
        if (!mayAutofill || !RegisterValidation.isValidPostalCode(plz, "DE")) {
            _uiState.update { it.copy(isCityLookupLoading = false) }
            return
        }
        cityLookupJob = viewModelScope.launch {
            _uiState.update { it.copy(isCityLookupLoading = true) }
            val city = cityLookupRepository.cityForPostalCode(plz)
            _uiState.update { state ->
                // Re-check guards: the PLZ may have changed or the user may have
                // typed a city while the request was in flight.
                val stillApplicable = state.zipCode.trim() == plz &&
                    (state.city.isBlank() || state.cityAutofilled)
                if (city != null && stillApplicable) {
                    state.copy(city = city, cityError = null, cityAutofilled = true, isCityLookupLoading = false)
                } else {
                    state.copy(isCityLookupLoading = false)
                }
            }
        }
    }

    // Optionale
    fun onBaptismChange(v: String) = _uiState.update { it.copy(baptism = v) }
    fun onGenderChange(v: String) = _uiState.update { it.copy(gender = v) }
    fun onBirthDateChange(v: String) =
        _uiState.update { it.copy(birthDate = v, birthDateError = null, bannerError = null) }

    fun onIdentityStatusChange(v: IdentityStatus) =
        _uiState.update { it.copy(identityStatus = v, identityStatusError = null, bannerError = null) }

    // The server caps 소속 at 100 characters; cap the input so it never comes back as a 400.
    fun onWorkOrSchoolChange(v: String) = _uiState.update { it.copy(workOrSchool = v.take(WORK_OR_SCHOOL_MAX)) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phoneNumber = PhoneNumber.sanitizeNational(v)) }
    fun onPhoneCountryChange(iso: String) {
        phoneCountryDefaulted = true
        _uiState.update { it.copy(phoneCountryIso = iso) }
    }

    /**
     * Sets the dial-code country from the device region, once. The screen calls
     * this on every fresh composition — a rotation included — and the
     * ViewModel outlives that, so without the guard a rotation would quietly
     * undo the member's own pick.
     */
    fun setDefaultPhoneCountry(iso: String?) {
        if (phoneCountryDefaulted) return
        phoneCountryDefaulted = true
        _uiState.update { it.copy(phoneCountryIso = Countries.byIsoOrDefault(iso.orEmpty()).iso) }
    }
    private var phoneCountryDefaulted = false

    fun onStreetChange(v: String) = _uiState.update { it.copy(street = v, streetError = null, bannerError = null) }
    fun onHouseNumberChange(v: String) = _uiState.update { it.copy(houseNumber = v, houseNumberError = null, bannerError = null) }

    fun register() {
        // Someone can type the date and hit the button without ever leaving the
        // field, so the normalisation that usually happens on focus loss has to
        // happen here too (#166).
        val normalisedBirthDate = BirthDateInput.normalise(_uiState.value.birthDate)
        if (normalisedBirthDate != _uiState.value.birthDate) {
            _uiState.update { it.copy(birthDate = normalisedBirthDate) }
        }
        val s = _uiState.value

        // 1. VALIDIERUNG — collect every field error so the user sees them all at once.
        val emailError = when {
            s.email.isBlank() -> RegisterFieldError.REQUIRED
            !RegisterValidation.isValidEmail(s.email) -> RegisterFieldError.INVALID_EMAIL
            else -> null
        }
        val passwordError = when {
            s.password.isBlank() -> RegisterFieldError.REQUIRED
            !s.passwordCriteria.allMet -> RegisterFieldError.PASSWORD_REQUIREMENTS
            else -> null
        }
        val firstNameError = RegisterFieldError.REQUIRED.takeIf { s.firstName.isBlank() }
        val lastNameError = RegisterFieldError.REQUIRED.takeIf { s.lastName.isBlank() }
        val cityError = when {
            s.city.isBlank() -> RegisterFieldError.REQUIRED
            !RegisterValidation.isValidCity(s.city) -> RegisterFieldError.INVALID_CITY
            else -> null
        }
        val zipCodeError = when {
            s.zipCode.isBlank() -> RegisterFieldError.REQUIRED
            !RegisterValidation.isValidPostalCode(s.zipCode) -> RegisterFieldError.INVALID_POSTCODE
            else -> null
        }
        // Address is optional as a whole, but the Straße/Hausnummer split must be
        // consistent: if either part is entered, both are required, and the house
        // number must contain a digit.
        val addressProvided = s.street.isNotBlank() || s.houseNumber.isNotBlank()
        val streetError = RegisterFieldError.REQUIRED.takeIf { addressProvided && s.street.isBlank() }
        val houseNumberError = when {
            !addressProvided -> null
            s.houseNumber.isBlank() -> RegisterFieldError.REQUIRED
            !RegisterValidation.isValidHouseNumber(s.houseNumber) -> RegisterFieldError.INVALID_HOUSE_NUMBER
            else -> null
        }
        val birthDateError = validateBirthDate(s.birthDate)
        val identityStatusError = RegisterFieldError.REQUIRED.takeIf { s.identityStatus == null }

        // First invalid field in visual order (last name renders above first name).
        val focusTarget = when {
            lastNameError != null -> RegisterField.LAST_NAME
            firstNameError != null -> RegisterField.FIRST_NAME
            emailError != null -> RegisterField.EMAIL
            passwordError != null -> RegisterField.PASSWORD
            birthDateError != null -> RegisterField.BIRTH_DATE
            identityStatusError != null -> RegisterField.IDENTITY_STATUS
            streetError != null -> RegisterField.STREET
            houseNumberError != null -> RegisterField.HOUSE_NUMBER
            zipCodeError != null -> RegisterField.ZIP_CODE
            cityError != null -> RegisterField.CITY
            else -> null
        }

        if (focusTarget != null) {
            _uiState.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    emailError = emailError,
                    cityError = cityError,
                    zipCodeError = zipCodeError,
                    passwordError = passwordError,
                    birthDateError = birthDateError,
                    identityStatusError = identityStatusError,
                    streetError = streetError,
                    houseNumberError = houseNumberError,
                    focusTarget = focusTarget,
                    bannerError = RegisterBanner.MissingRequired,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, bannerError = null) }

            // 2. MAPPING (String -> Nullable für Backend)
            val request = RegisterRequest(
                firstName = s.firstName,
                lastName = s.lastName,
                email = s.email,
                password = s.password,
                city = s.city,
                baptism = s.baptism.ifBlank { null },
                gender = s.gender.ifBlank { null },
                // Both are validated above, so the non-null forms are safe here.
                birthDate = s.birthDate.replace('.', '-'),
                identityStatus = checkNotNull(s.identityStatus).name,
                phoneNumber = PhoneNumber.toE164(
                    dialCode = Countries.byIsoOrDefault(s.phoneCountryIso).dialCode,
                    national = s.phoneNumber,
                ),
                street = s.street.ifBlank { null },
                houseNumber = s.houseNumber.ifBlank { null },
                zipCode = s.zipCode.ifBlank { null },
                workOrSchool = s.workOrSchool.trim().ifBlank { null },
            )

            // 1. REGISTRIERUNG AUFRUFEN
            val result = authRepository.register(request)

            result.onSuccess {
                // Backend sends verification mail. Login continues in the browser;
                // the registration password is never replayed as a login grant.
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, navigateTo = NavRoute.Login,
                        loginNotice = LoginRoute.NOTICE_VERIFY_EMAIL, password = "", bannerError = null)
                }
            }.onFailure { exception ->
                if ((exception as? RegisterException)?.isServerError == true) {
                    // Not the member's input: no banner pointing at the form,
                    // and the server's message stays out of sight. The fields
                    // are kept so a later retry needs no retyping.
                    _uiState.update {
                        it.copy(isLoading = false, bannerError = null, showUnavailableDialog = true)
                    }
                    return@onFailure
                }
                // Show a clean backend message if we have one, else a localized generic.
                val banner = (exception as? RegisterException)?.userMessage
                    ?.takeIf { it.isNotBlank() }
                    ?.let { RegisterBanner.ServerMessage(it) }
                    ?: RegisterBanner.Generic
                _uiState.update { it.copy(isLoading = false, bannerError = banner) }
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateTo = null, loginNotice = null) }
    }

    fun onUnavailableDialogDismissed() {
        _uiState.update { it.copy(showUnavailableDialog = false) }
    }

    /** Called by the UI once it has focused/scrolled to the invalid field. */
    fun onFocusHandled() {
        _uiState.update { it.copy(focusTarget = null) }
    }

}

internal const val WORK_OR_SCHOOL_MAX = 100

/**
 * Returns a field error, or null if the date is valid. Required since #282, and
 * shared with the profile edit so both forms reject the same input (#283).
 */
internal fun validateBirthDate(date: String): RegisterFieldError? {
    if (date.isBlank()) return RegisterFieldError.REQUIRED
    if (date.length < 10) return RegisterFieldError.DATE_INCOMPLETE
    return try {
        val parts = date.split(".")
        LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        null // date is valid, no error
    } catch (e: Exception) {
        RegisterFieldError.DATE_INVALID
    }
}

