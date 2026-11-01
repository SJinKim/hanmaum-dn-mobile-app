package com.hanmaum.dn.mobile.features.profile.presentation

import com.hanmaum.dn.mobile.core.i18n.UiMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.core.network.invalidateBearerCache
import io.ktor.client.HttpClient
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
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileAvatar
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageException
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileLoadException
import com.hanmaum.dn.mobile.features.profile.domain.ProfileAvatarCache
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePick
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePicker
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparation
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val tokenStorage: TokenStorage,
    private val biometricVault: BiometricVault,
    private val notificationRepository: NotificationRepository,
    private val pushManager: PushManager,
    private val authPreferences: AuthPreferences,
    private val verseRecordRepository: VerseRecordRepository,
    private val httpClient: HttpClient,
    private val cityLookupRepository: CityLookupRepository,
    private val imagePreparer: ProfileImagePreparer,
    private val avatarCache: ProfileAvatarCache,
) : ViewModel() {

    init {
        // Files a killed process left behind; nothing else is in that folder.
        imagePreparer.purgeStale()
    }

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
            // Never beside a mutation, and never over an unsaved draft.
            if (current is ProfileUiState.Success && current.blocksRefresh) return@launch
            val hadData = current is ProfileUiState.Success
            if (!hadData) _uiState.value = ProfileUiState.Loading
            memberRepository.getMyProfile().fold(
                onSuccess = { fresh ->
                    val now = _uiState.value
                    if (now !is ProfileUiState.Success) {
                        _uiState.value = ProfileUiState.Success(fresh)
                        return@fold
                    }
                    if (now.blocksRefresh) return@fold
                    val merged = keepNewerAvatar(now.profile, fresh)
                    evictIfReplaced(now.profile, merged)
                    _uiState.value = ProfileUiState.Success(merged).copy(
                        avatarActivity = now.avatarActivity.takeUnless { it is AvatarActivity.Uncertain }
                            ?: AvatarActivity.Idle,
                        avatarPreview = now.avatarPreview,
                    )
                },
                onFailure = { error ->
                    if (!hadData) {
                        _uiState.value = ProfileUiState.Error(error.message ?: "Profile load failed")
                    } else {
                        edit { it.copy(isOffline = error !is ProfileLoadException) }
                    }
                },
            )
        }
    }

    private val ProfileUiState.Success.blocksRefresh: Boolean
        get() = isDirty || isSaving || avatarActivity.isBusy

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
        // The photo saved on its own, so cancelling the form neither undoes
        // a confirmed upload nor stops one that is still running.
        _uiState.value = ProfileUiState.Success(profile = current.profile).copy(
            avatarActivity = current.avatarActivity,
            avatarPreview = current.avatarPreview,
            isOffline = current.isOffline,
        )
    }

    // Editing a field clears its own error, as on the registration form.
    fun updateBirthDate(value: String) = edit { it.copy(editBirthDate = value, birthDateError = null) }

    fun consumeSaveSuccess() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (current.saveSuccess) _uiState.value = current.copy(saveSuccess = false)
    }

    fun updatePhone(value: String) = edit { it.copy(editPhone = PhoneNumber.sanitizeNational(value)) }

    fun updatePhoneCountry(iso: String) = edit { it.copy(editPhoneCountryIso = iso) }

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

    // update{} rather than a read-then-write: upload progress arrives off the main thread.
    private fun edit(change: (ProfileUiState.Success) -> ProfileUiState.Success) {
        _uiState.update { state -> (state as? ProfileUiState.Success)?.let(change) ?: state }
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
        abandonAvatarWork()
        imagePreparer.purgeStale()
        viewModelScope.launch {
            // Best-effort: stop push to this device for the signed-out account.
            // Must run before the token clear or the call goes out unauthenticated.
            pushManager.currentToken()?.let { notificationRepository.deleteDeviceToken(it) }
            tokenStorage.clear()
            httpClient.invalidateBearerCache()
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
        // One mutation at a time: the PATCH waits for the photo to settle.
        if ((_uiState.value as? ProfileUiState.Success)?.avatarActivity?.isBusy == true) return
        val current = validated() ?: return
        _uiState.value = current.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            memberRepository.updateMyProfile(
                phoneNumber = clearable(current.editPhoneE164, current.profile.phoneNumber),
                birthDate = current.editBirthDate.replace('.', '-'),
                identityStatus = checkNotNull(current.editIdentityStatus).name,
                // The server trims 소속 and stores a blank one as null, so blank clears it.
                workOrSchool = current.editWorkOrSchool.trim(),
                street = clearable(current.editStreet.trim(), current.profile.street),
                houseNumber = clearable(current.editHouseNumber.trim(), current.profile.houseNumber),
                zipCode = current.editZipCode.trim(),
                city = current.editCity.trim(),
            ).fold(
                onSuccess = { updated ->
                    val now = _uiState.value as? ProfileUiState.Success
                    _uiState.value = ProfileUiState.Success(keepNewerAvatar(now?.profile ?: current.profile, updated))
                        .copy(saveSuccess = true, avatarActivity = now?.avatarActivity ?: AvatarActivity.Idle)
                },
                onFailure = { err ->
                    _uiState.value = current.copy(isSaving = false, saveError = UiMessage.SAVE_PROFILE_FAILED)
                },
            )
        }
    }

    // ─────────────────────────── photo ───────────────────────────

    private var avatarJob: Job? = null

    /**
     * Bumped by every new photo action, cancel and logout. A callback or a
     * response carrying an older number belongs to an abandoned action and
     * is dropped — it must not overwrite what the member did since.
     */
    @Volatile
    private var avatarOperation = 0L

    /** Signed URLs already renewed once, so an image that keeps failing cannot loop. */
    private val renewedAvatarUrls = mutableSetOf<String>()

    /** Opens the system picker, prepares the pick and uploads it. Nothing is saved until the server answers. */
    fun pickAvatar(picker: ProfileImagePicker) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (!current.canChangeAvatar) return
        releasePreview()
        val op = ++avatarOperation
        setAvatar(op, AvatarActivity.Picking)
        avatarJob = viewModelScope.launch {
            when (val pick = picker.pick()) {
                ProfileImagePick.Cancelled -> setAvatar(op, AvatarActivity.Idle)
                ProfileImagePick.Unreadable ->
                    setAvatar(op, AvatarActivity.Failed(ProfileImageFailure.UNREADABLE, canRetry = false))
                is ProfileImagePick.Selected -> {
                    if (op != avatarOperation) {
                        pick.image.release()
                        return@launch
                    }
                    setAvatar(op, AvatarActivity.Preparing)
                    val prepared = try {
                        imagePreparer.prepare(pick.image)
                    } finally {
                        // The original is not needed past this point, whatever happened.
                        pick.image.release()
                    }
                    when (prepared) {
                        is ProfileImagePreparation.Failed ->
                            setAvatar(op, AvatarActivity.Failed(prepared.failure, canRetry = false))
                        is ProfileImagePreparation.Ready -> {
                            if (op != avatarOperation) {
                                prepared.image.release()
                                return@launch
                            }
                            edit { it.copy(avatarPreview = prepared.image) }
                            upload(op, prepared.image)
                        }
                    }
                }
            }
        }
    }

    /** Sends the same prepared file again after a retryable failure. */
    fun retryAvatarUpload() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        val failed = current.avatarActivity as? AvatarActivity.Failed ?: return
        val image = current.avatarPreview ?: return
        if (!failed.canRetry || current.isSaving || current.isOffline) return
        val op = ++avatarOperation
        avatarJob = viewModelScope.launch { upload(op, image) }
    }

    /**
     * Stops the local work. Before the upload started nothing can have
     * changed. Once it started, the server may already have stored the
     * photo — the app asks it instead of claiming the old one stayed, and
     * sends no compensating DELETE.
     */
    fun cancelAvatarChange() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        val activity = current.avatarActivity
        val reachedServer = activity is AvatarActivity.Uploading || activity is AvatarActivity.AwaitingServer
        if (!reachedServer && activity !is AvatarActivity.Picking && activity !is AvatarActivity.Preparing) return
        avatarJob?.cancel()
        val op = ++avatarOperation
        releasePreview()
        if (reachedServer) {
            reconcile(op, versionBefore = current.profile.profileImageVersion)
        } else {
            setAvatar(op, AvatarActivity.Idle)
        }
    }

    /** Asks the server again after an unanswered reconcile. */
    fun recheckAvatar() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (current.avatarActivity !is AvatarActivity.Uncertain) return
        reconcile(++avatarOperation, versionBefore = current.profile.profileImageVersion)
    }

    fun dismissAvatarError() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (current.avatarActivity !is AvatarActivity.Failed) return
        avatarOperation++
        releasePreview()
        edit { it.copy(avatarActivity = AvatarActivity.Idle) }
    }

    fun requestAvatarDelete() = edit {
        if (it.canChangeAvatar && it.avatar != null) it.copy(showAvatarDeleteConfirm = true) else it
    }

    fun dismissAvatarDelete() = edit { it.copy(showAvatarDeleteConfirm = false) }

    fun confirmAvatarDelete() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        edit { it.copy(showAvatarDeleteConfirm = false) }
        if (!current.canChangeAvatar || current.avatar == null) return
        releasePreview()
        val op = ++avatarOperation
        val versionBefore = current.profile.profileImageVersion
        setAvatar(op, AvatarActivity.Deleting)
        avatarJob = viewModelScope.launch {
            memberRepository.deleteProfileImage().fold(
                onSuccess = { fresh ->
                    if (op != avatarOperation) return@fold
                    applyConfirmed(fresh)
                    setAvatar(op, AvatarActivity.Idle)
                },
                onFailure = { error ->
                    val failure = (error as? ProfileImageException)?.failure ?: ProfileImageFailure.UNKNOWN
                    if (failure == ProfileImageFailure.NETWORK) {
                        reconcile(op, versionBefore)
                    } else {
                        setAvatar(op, AvatarActivity.Failed(failure, canRetry = false))
                    }
                },
            )
        }
    }

    /**
     * The signed GET of the avatar was refused (401/403): it most likely
     * expired. Renew it through members/me once per URL; the cache key stays
     * the same because the version did not change.
     */
    fun onAvatarLoadFailed(avatar: ProfileAvatar, httpStatus: Int?) {
        if (httpStatus != 401 && httpStatus != 403) return
        val url = avatar.url ?: return
        if (!renewedAvatarUrls.add(url)) return
        viewModelScope.launch {
            memberRepository.getMyProfile().onSuccess { fresh ->
                val current = _uiState.value as? ProfileUiState.Success ?: return@onSuccess
                if (current.avatarActivity.isBusy) return@onSuccess
                applyConfirmed(fresh)
            }
        }
    }

    private suspend fun upload(op: Long, image: PreparedProfileImage) {
        val versionBefore = (_uiState.value as? ProfileUiState.Success)?.profile?.profileImageVersion ?: 0L
        var started = false
        setAvatar(op, AvatarActivity.Uploading(0, image.byteLength))
        val result = memberRepository.uploadProfileImage(image) { sent, total ->
            if (sent > 0) started = true
            val length = total ?: image.byteLength
            setAvatar(op, if (sent >= length) AvatarActivity.AwaitingServer else AvatarActivity.Uploading(sent, total))
        }
        if (op != avatarOperation) return
        result.fold(
            onSuccess = { fresh ->
                applyConfirmed(fresh)
                releasePreview()
                setAvatar(op, AvatarActivity.Idle)
            },
            onFailure = { error ->
                val failure = (error as? ProfileImageException)?.failure ?: ProfileImageFailure.UNKNOWN
                when {
                    // Bytes left the device: the server may have stored them.
                    failure == ProfileImageFailure.NETWORK && started -> reconcile(op, versionBefore)
                    failure in RETRYABLE -> setAvatar(op, AvatarActivity.Failed(failure, canRetry = true))
                    else -> {
                        releasePreview()
                        setAvatar(op, AvatarActivity.Failed(failure, canRetry = false))
                    }
                }
            },
        )
    }

    /**
     * Reads the profile to learn what the server kept. A higher version means
     * the photo was stored after all. A GET can still miss a commit that
     * lands later, which is why the next profile entry loads again.
     */
    private fun reconcile(op: Long, versionBefore: Long) {
        setAvatar(op, AvatarActivity.Reconciling)
        viewModelScope.launch {
            memberRepository.getMyProfile().fold(
                onSuccess = { fresh ->
                    if (op != avatarOperation) return@fold
                    applyConfirmed(fresh)
                    val preview = (_uiState.value as? ProfileUiState.Success)?.avatarPreview
                    when {
                        fresh.profileImageVersion > versionBefore -> {
                            releasePreview()
                            setAvatar(op, AvatarActivity.Idle)
                        }
                        preview != null ->
                            setAvatar(op, AvatarActivity.Failed(ProfileImageFailure.NETWORK, canRetry = true))
                        else -> setAvatar(op, AvatarActivity.Idle)
                    }
                },
                onFailure = { error ->
                    if (op != avatarOperation) return@fold
                    edit {
                        it.copy(avatarActivity = AvatarActivity.Uncertain, isOffline = error !is ProfileLoadException)
                    }
                },
            )
        }
    }

    /** Takes the image fields of a server answer. A lower version never replaces a higher one. */
    private fun applyConfirmed(fresh: MemberResponse) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        val merged = keepNewerAvatar(current.profile, fresh)
        evictIfReplaced(current.profile, merged)
        // Only the photo fields: the rest of the profile is the base of the contact draft.
        edit {
            it.copy(
                profile = it.profile.copy(
                    profileImageUrl = merged.profileImageUrl,
                    profileImageVersion = merged.profileImageVersion,
                ),
                isOffline = false,
            )
        }
    }

    private fun keepNewerAvatar(current: MemberResponse, fresh: MemberResponse): MemberResponse =
        if (fresh.publicId == current.publicId && fresh.profileImageVersion < current.profileImageVersion) {
            fresh.copy(profileImageUrl = current.profileImageUrl, profileImageVersion = current.profileImageVersion)
        } else {
            fresh
        }

    private fun evictIfReplaced(old: MemberResponse, new: MemberResponse) {
        if (old.profileImageVersion != new.profileImageVersion || old.publicId != new.publicId) {
            avatarCache.evict(ProfileAvatar.cacheKey(old.publicId, old.profileImageVersion))
        }
    }

    private fun setAvatar(op: Long, activity: AvatarActivity) {
        if (op == avatarOperation) edit { it.copy(avatarActivity = activity) }
    }

    private fun releasePreview() {
        val preview = (_uiState.value as? ProfileUiState.Success)?.avatarPreview ?: return
        preview.release()
        edit { it.copy(avatarPreview = null) }
    }

    private fun abandonAvatarWork() {
        avatarJob?.cancel()
        avatarOperation++
        releasePreview()
    }

    override fun onCleared() {
        abandonAvatarWork()
        super.onCleared()
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

/** Failures the same prepared file may get past on a second try. */
private val RETRYABLE = setOf(
    ProfileImageFailure.NETWORK,
    ProfileImageFailure.STORAGE_UNAVAILABLE,
    ProfileImageFailure.UNKNOWN,
)
