package com.hanmaum.dn.mobile.features.profile.presentation

import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: MemberResponse,
        val editPhone: String = profile.phoneNumber ?: "",
        val editImageUrl: String = profile.profileImageUrl ?: "",
        val editBirthDate: String = profile.birthDate?.replace('-', '.') ?: "",
        val editStreet: String = profile.street ?: "",
        val editHouseNumber: String = profile.houseNumber ?: "",
        val editZipCode: String = profile.zipCode ?: "",
        val editCity: String = profile.city ?: "",
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
            get() = editPhone != (profile.phoneNumber ?: "") ||
                editImageUrl != (profile.profileImageUrl ?: "") ||
                editBirthDate != (profile.birthDate?.replace('-', '.') ?: "") ||
                editStreet != (profile.street ?: "") ||
                editHouseNumber != (profile.houseNumber ?: "") ||
                editZipCode != (profile.zipCode ?: "") ||
                editCity != (profile.city ?: "")

        /** False for a partial birthdate (e.g. "1992.1") — never silently truncated on save. */
        val isBirthDateValid: Boolean
            get() = editBirthDate.isEmpty() || editBirthDate.length == 10
    }
    data class Error(val message: String) : ProfileUiState()
}
