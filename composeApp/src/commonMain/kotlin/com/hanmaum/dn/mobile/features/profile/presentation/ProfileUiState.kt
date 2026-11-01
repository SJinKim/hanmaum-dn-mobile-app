package com.hanmaum.dn.mobile.features.profile.presentation

import com.hanmaum.dn.mobile.core.i18n.UiMessage
import com.hanmaum.dn.mobile.features.login.domain.model.Countries
import com.hanmaum.dn.mobile.features.login.domain.model.IdentityStatus
import com.hanmaum.dn.mobile.features.login.domain.model.PhoneNumber
import com.hanmaum.dn.mobile.features.login.presentation.RegisterFieldError
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileAvatar
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure

/**
 * What the photo is doing, beside the confirmed [ProfileUiState.Success.profile].
 * Orthogonal to the contact form: the photo saves on its own via PUT/DELETE.
 */
sealed interface AvatarActivity {
    data object Idle : AvatarActivity
    data object Picking : AvatarActivity
    data object Preparing : AvatarActivity
    data class Uploading(val sent: Long, val total: Long?) : AvatarActivity {
        /** null when the length is unknown — the indicator is then indeterminate. */
        val fraction: Float? get() = total?.takeIf { it > 0 }?.let { (sent.toFloat() / it).coerceIn(0f, 1f) }
    }
    /** Every byte is out; the server is cropping and storing. Not saved yet. */
    data object AwaitingServer : AvatarActivity
    data object Deleting : AvatarActivity
    /** An upload was cut off after it started; asking the server what it kept. */
    data object Reconciling : AvatarActivity
    /** That question could not be answered either. Never claims the old photo is unchanged. */
    data object Uncertain : AvatarActivity
    data class Failed(val failure: ProfileImageFailure, val canRetry: Boolean) : AvatarActivity

    val isBusy: Boolean
        get() = this is Picking || this is Preparing || this is Uploading ||
            this is AwaitingServer || this is Deleting || this is Reconciling
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: MemberResponse,
        /** National digits only; the dial code lives in [editPhoneCountryIso], as at registration. */
        val editPhone: String = profile.storedPhone()?.second ?: "",
        val editPhoneCountryIso: String = profile.storedPhone()?.first ?: Countries.DEFAULT_ISO,
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
        val saveError: UiMessage? = null,
        val saveSuccess: Boolean = false,
        val avatarActivity: AvatarActivity = AvatarActivity.Idle,
        /** The picked, prepared photo while it is unsaved — shown marked as such, never as the avatar. */
        val avatarPreview: PreparedProfileImage? = null,
        val showAvatarDeleteConfirm: Boolean = false,
        /** The last profile call never reached the server. Photo changes wait until it does. */
        val isOffline: Boolean = false,
    ) : ProfileUiState() {
        /** The confirmed avatar, or null when the server has none to show. */
        val avatar: ProfileAvatar?
            get() = profile.profileImageUrl?.let { ProfileAvatar(profile.publicId, profile.profileImageVersion, it) }

        /** Photo actions are off while any mutation runs or the server is out of reach. */
        val canChangeAvatar: Boolean
            get() = !avatarActivity.isBusy && avatarActivity !is AvatarActivity.Uncertain && !isSaving && !isOffline

        /** True when any edit field differs from the loaded profile — gates the Save button. */
        val isDirty: Boolean
            get() = isPhoneDirty ||
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
