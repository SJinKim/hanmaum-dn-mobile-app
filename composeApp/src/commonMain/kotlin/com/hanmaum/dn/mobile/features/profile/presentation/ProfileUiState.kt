package com.hanmaum.dn.mobile.features.profile.presentation

import com.hanmaum.dn.mobile.features.login.domain.model.Countries
import com.hanmaum.dn.mobile.features.login.domain.model.IdentityStatus
import com.hanmaum.dn.mobile.features.login.domain.model.PhoneNumber
import com.hanmaum.dn.mobile.features.login.presentation.RegisterFieldError
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: MemberResponse,
        /** National digits only; the dial code lives in [editPhoneCountryIso], as at registration. */
        val editPhone: String = profile.storedPhone()?.second ?: "",
        val editPhoneCountryIso: String = profile.storedPhone()?.first ?: Countries.DEFAULT_ISO,
        val editImageUrl: String = profile.profileImageUrl ?: "",
        val editBirthDate: String = profile.storedBirthDate(),
        val editIdentityStatus: IdentityStatus? = profile.storedIdentityStatus(),
        val editWorkOrSchool: String = profile.workOrSchool ?: "",
        val editStreet: String = profile.street ?: "",
        val editHouseNumber: String = profile.houseNumber ?: "",
        val editZipCode: String = profile.zipCode ?: "",
        val editCity: String = profile.city ?: "",
        /**
         * True once the member typed the city in this edit. Until then the city —
         * loaded from the profile or filled by the postcode lookup — may be
         * replaced by a lookup for a new postcode.
         */
        val cityTypedByHand: Boolean = false,
        val isCityLookupLoading: Boolean = false,
        // The fields that are required at registration stay required here (#283).
        val birthDateError: RegisterFieldError? = null,
        val identityStatusError: RegisterFieldError? = null,
        val streetError: RegisterFieldError? = null,
        val houseNumberError: RegisterFieldError? = null,
        val zipCodeError: RegisterFieldError? = null,
        val cityError: RegisterFieldError? = null,
        /**
         * The redesigned profile edits in place instead of pushing a separate
         * screen, so the same state carries both modes.
         */
        val isEditing: Boolean = false,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val saveSuccess: Boolean = false,
    ) : ProfileUiState() {
        /** True when any edit field differs from the loaded profile — gates the Save button. */
        val isDirty: Boolean
            get() = isPhoneDirty ||
                editImageUrl != (profile.profileImageUrl ?: "") ||
                editBirthDate != profile.storedBirthDate() ||
                editIdentityStatus != profile.storedIdentityStatus() ||
                editWorkOrSchool != (profile.workOrSchool ?: "") ||
                editStreet != (profile.street ?: "") ||
                editHouseNumber != (profile.houseNumber ?: "") ||
                editZipCode != (profile.zipCode ?: "") ||
                editCity != (profile.city ?: "")

        // Compared as E.164 so a legacy number stored without "+" is not dirty on open.
        private val isPhoneDirty: Boolean
            get() = editPhoneE164 != profile.storedPhone()?.let { (iso, national) ->
                PhoneNumber.toE164(Countries.byIsoOrDefault(iso).dialCode, national)
            }

        /** The number as it is sent: E.164, or null once the field is cleared. */
        val editPhoneE164: String?
            get() = PhoneNumber.toE164(Countries.byIsoOrDefault(editPhoneCountryIso).dialCode, editPhone)

        val hasFieldError: Boolean
            get() = listOf(birthDateError, identityStatusError, streetError, houseNumberError, zipCodeError, cityError)
                .any { it != null }
    }
    data class Error(val message: String) : ProfileUiState()
}

private fun MemberResponse.storedPhone(): Pair<String, String>? = PhoneNumber.splitE164(phoneNumber)

private fun MemberResponse.storedBirthDate(): String = birthDate?.replace('-', '.') ?: ""

private fun MemberResponse.storedIdentityStatus(): IdentityStatus? =
    IdentityStatus.entries.firstOrNull { it.name == identityStatus }
