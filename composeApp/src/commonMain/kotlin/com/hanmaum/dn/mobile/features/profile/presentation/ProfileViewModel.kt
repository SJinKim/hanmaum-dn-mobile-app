package com.hanmaum.dn.mobile.features.profile.presentation

import com.hanmaum.dn.mobile.core.i18n.UiMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentSessionRepository
import com.hanmaum.dn.mobile.features.login.domain.model.BirthDateInput
import com.hanmaum.dn.mobile.features.login.domain.model.IdentityStatus
import com.hanmaum.dn.mobile.features.login.domain.model.PhoneNumber
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterValidation
import com.hanmaum.dn.mobile.features.login.domain.repository.CityLookupRepository
import com.hanmaum.dn.mobile.features.login.presentation.RegisterFieldError
import com.hanmaum.dn.mobile.features.login.presentation.WORK_OR_SCHOOL_MAX
import com.hanmaum.dn.mobile.features.login.presentation.validateBirthDate
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecords
import com.hanmaum.dn.mobile.features.verse.domain.repository.VerseRecordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val session: ConsentSessionRepository,
    private val verseRecordRepository: VerseRecordRepository,
    private val cityLookupRepository: CityLookupRepository,
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    /**
     * The two 말씀 streaks, kept beside [uiState] rather than inside it.
     *
     * They are orthogonal to loading the profile: the tiles show a dash until
     * they arrive, and a failed streak call must not turn the whole screen into
     * an error. Folding them into the sealed state would tie the two together.
     */
    private val _verseRecords = MutableStateFlow<VerseRecords?>(null)
    val verseRecords: StateFlow<VerseRecords?> = _verseRecords.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Loads (or reloads) the profile. Driven by the screen on every entry so a
     * profile edited elsewhere (e.g. the web app) stays current without a
     * re-login. Refreshes silently when data is already shown, skips entirely
     * while an edit is in progress (so it is never clobbered), and keeps the
     * current data on a transient refresh failure.
     */
    private fun loadVerseRecords() {
        viewModelScope.launch {
            verseRecordRepository.getRecords()
                .onSuccess { _verseRecords.value = it }
            // onFailure: the tiles keep their dash.
        }
    }

    fun loadProfile() {
        loadVerseRecords()
        viewModelScope.launch {
            val current = _uiState.value
            if (current is ProfileUiState.Success && current.isDirty) return@launch
            val hadData = current is ProfileUiState.Success
            if (!hadData) _uiState.value = ProfileUiState.Loading
            memberRepository.getMyProfile().fold(
                onSuccess = { _uiState.value = ProfileUiState.Success(it) },
                onFailure = { if (!hadData) _uiState.value = ProfileUiState.Error(it.message ?: "Profile load failed") },
            )
        }
    }

    fun startEditing() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = current.copy(isEditing = true, saveError = null)
    }

    /**
     * Leaves edit mode and throws away the draft.
     *
     * The edit fields are reset from the loaded profile rather than merely
     * hidden — otherwise re-entering edit mode would show the abandoned draft
     * as if it had been saved.
     */
    fun cancelEditing() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        cityLookupJob?.cancel()
        _uiState.value = ProfileUiState.Success(profile = current.profile)
    }

    // Editing a field clears its own error, as on the registration form.
    fun updateBirthDate(value: String) = edit { it.copy(editBirthDate = value, birthDateError = null) }

    fun consumeSaveSuccess() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (current.saveSuccess) _uiState.value = current.copy(saveSuccess = false)
    }

    fun updatePhone(value: String) = edit { it.copy(editPhone = PhoneNumber.sanitizeNational(value)) }

    fun updatePhoneCountry(iso: String) = edit { it.copy(editPhoneCountryIso = iso) }

    fun updateImageUrl(value: String) = edit { it.copy(editImageUrl = value) }

    fun updateIdentityStatus(value: IdentityStatus) =
        edit { it.copy(editIdentityStatus = value, identityStatusError = null) }

    // The server caps 소속 at 100 characters; cap the input so it never comes back as a 400.
    fun updateWorkOrSchool(value: String) = edit { it.copy(editWorkOrSchool = value.take(WORK_OR_SCHOOL_MAX)) }

    // Street and house number are checked as a pair, so typing in one clears both.
    fun updateStreet(value: String) =
        edit { it.copy(editStreet = value, streetError = null, houseNumberError = null) }

    fun updateHouseNumber(value: String) =
        edit { it.copy(editHouseNumber = value, streetError = null, houseNumberError = null) }

    fun updateZipCode(value: String) {
        edit { it.copy(editZipCode = value, zipCodeError = null) }
        maybeAutofillCity(value)
    }

    fun updateCity(value: String) =
        // The member is taking over the city — stop autofilling over their input.
        edit { it.copy(editCity = value, cityError = null, cityTypedByHand = true) }

    private fun edit(change: (ProfileUiState.Success) -> ProfileUiState.Success) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = change(current)
    }

    // Tracks the in-flight PLZ->city lookup so a newer postcode cancels the older one.
    private var cityLookupJob: Job? = null

    /**
     * The same autofill as registration (#283): on a complete German PLZ the
     * city is looked up and filled in. A city the member typed in this edit is
     * never overwritten, and a failed lookup is silent.
     *
     * Unlike registration the city starts filled from the profile. That value
     * counts as not typed, so moving to a new postcode brings its city along
     * instead of leaving the old one next to it.
     */
    private fun maybeAutofillCity(zip: String) {
        cityLookupJob?.cancel()
        val plz = zip.trim()
        val current = _uiState.value as? ProfileUiState.Success ?: return
        val mayAutofill = current.editCity.isBlank() || !current.cityTypedByHand
        if (!mayAutofill || !RegisterValidation.isValidPostalCode(plz, "DE")) {
            edit { it.copy(isCityLookupLoading = false) }
            return
        }
        cityLookupJob = viewModelScope.launch {
            edit { it.copy(isCityLookupLoading = true) }
            val city = cityLookupRepository.cityForPostalCode(plz)
            edit { state ->
                // Re-check guards: the PLZ may have changed or the member may have
                // typed a city while the request was in flight.
                val stillApplicable = state.isEditing && state.editZipCode.trim() == plz &&
                    (state.editCity.isBlank() || !state.cityTypedByHand)
                if (city != null && stillApplicable) {
                    state.copy(editCity = city, cityError = null, isCityLookupLoading = false)
                } else {
                    state.copy(isCityLookupLoading = false)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Device-token cleanup, token clear and cache invalidation live in the
            // session repository, shared with the consent lock screen's logout.
            session.logout()
            // The Face ID arming deliberately survives. Signing out and back in
            // is the ordinary way to end a session, and disarming here left the
            // member with a login screen that never offered Face ID — there was
            // no way to reach it at all (#218). What stays behind is a refresh
            // token the OS releases only against this member's face; another
            // member signing in with a password clears it (LoginViewModel).
            _loggedOut.value = true
        }
    }

    fun saveProfile() {
        // Someone can type the date and tap 저장 without leaving the field, so the
        // normalisation that usually happens on focus loss happens here too.
        edit { it.copy(editBirthDate = BirthDateInput.normalise(it.editBirthDate)) }
        val current = validated() ?: return
        _uiState.value = current.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            memberRepository.updateMyProfile(
                phoneNumber = clearable(current.editPhoneE164, current.profile.phoneNumber),
                profileImageUrl = current.editImageUrl.ifBlank { null },
                birthDate = current.editBirthDate.replace('.', '-'),
                identityStatus = checkNotNull(current.editIdentityStatus).name,
                // The server trims 소속 and stores a blank one as null, so blank clears it.
                workOrSchool = current.editWorkOrSchool.trim(),
                street = clearable(current.editStreet.trim(), current.profile.street),
                houseNumber = clearable(current.editHouseNumber.trim(), current.profile.houseNumber),
                zipCode = current.editZipCode.trim(),
                city = current.editCity.trim(),
            ).fold(
                onSuccess = { updated -> _uiState.value = ProfileUiState.Success(updated).copy(saveSuccess = true) },
                onFailure = { err ->
                    _uiState.value = current.copy(isSaving = false, saveError = UiMessage.SAVE_PROFILE_FAILED)
                },
            )
        }
    }

    /**
     * PATCH skips null, so clearing a stored value has to send "". A field that
     * was empty all along stays null rather than turning into "" on the server.
     */
    private fun clearable(value: String?, stored: String?): String? = when {
        !value.isNullOrBlank() -> value
        stored.isNullOrBlank() -> null
        else -> ""
    }

    /**
     * Applies the registration rules to the draft (#283): 생년월일, 신분,
     * 우편번호 and 도시 are required, and 도로명/번지 are optional but only
     * as a pair. Returns the state to save, or null after showing every error.
     */
    private fun validated(): ProfileUiState.Success? {
        val s = _uiState.value as? ProfileUiState.Success ?: return null
        val addressProvided = s.editStreet.isNotBlank() || s.editHouseNumber.isNotBlank()
        val checked = s.copy(
            birthDateError = validateBirthDate(s.editBirthDate),
            identityStatusError = RegisterFieldError.REQUIRED.takeIf { s.editIdentityStatus == null },
            streetError = RegisterFieldError.REQUIRED.takeIf { addressProvided && s.editStreet.isBlank() },
            houseNumberError = when {
                !addressProvided -> null
                s.editHouseNumber.isBlank() -> RegisterFieldError.REQUIRED
                !RegisterValidation.isValidHouseNumber(s.editHouseNumber) -> RegisterFieldError.INVALID_HOUSE_NUMBER
                else -> null
            },
            zipCodeError = when {
                s.editZipCode.isBlank() -> RegisterFieldError.REQUIRED
                !RegisterValidation.isValidPostalCode(s.editZipCode.trim()) -> RegisterFieldError.INVALID_POSTCODE
                else -> null
            },
            cityError = when {
                s.editCity.isBlank() -> RegisterFieldError.REQUIRED
                !RegisterValidation.isValidCity(s.editCity) -> RegisterFieldError.INVALID_CITY
                else -> null
            },
        )
        _uiState.value = checked
        return checked.takeUnless { it.hasFieldError }
    }
}
