package com.hanmaum.dn.mobile.features.profile

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.features.consent.FakeConsentSession
import com.hanmaum.dn.mobile.features.login.domain.model.IdentityStatus
import com.hanmaum.dn.mobile.features.login.domain.repository.CityLookupRepository
import com.hanmaum.dn.mobile.features.login.presentation.RegisterFieldError
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageException
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileLoadException
import com.hanmaum.dn.mobile.features.profile.domain.PickedProfileImage
import com.hanmaum.dn.mobile.features.profile.domain.ProfileAvatarCache
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePick
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePicker
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparation
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparer
import com.hanmaum.dn.mobile.features.profile.presentation.AvatarActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.io.Buffer
import kotlinx.io.Source
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.verse.FakeVerseRecordRepository
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileUiState
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle

private class FakeMemberRepository : MemberRepository {
    var profile = MemberResponse(
        publicId = "u1",
        firstName = "Seungjin",
        lastName = "Kim",
        status = MemberStatus.ACTIVE,
        division = "2교구",
        street = "Musterstraße",
        houseNumber = "12",
        zipCode = "50667",
        city = "Köln",
        birthDate = "1992-12-07",
        identityStatus = "EMPLOYEE",
    )

    data class UpdateArgs(
        val phoneNumber: String?,
        val birthDate: String?,
        val street: String?,
        val houseNumber: String?,
        val zipCode: String?,
        val city: String?,
        val identityStatus: String?,
        val workOrSchool: String?,
    )

    var lastUpdate: UpdateArgs? = null

    /** Queued answers for getMyProfile; empty means "the stored profile". */
    val profileResults = ArrayDeque<Result<MemberResponse>>()
    var profileCalls = 0

    override suspend fun getMyProfile(): Result<MemberResponse> {
        profileCalls++
        return profileResults.removeFirstOrNull() ?: Result.success(profile)
    }

    val uploadResults = ArrayDeque<Result<MemberResponse>>()
    val uploadedImages = mutableListOf<PreparedProfileImage>()
    /** When set, an upload reports half its bytes and then waits here. */
    var uploadGate: CompletableDeferred<Unit>? = null
    /** When set, an upload reports all its bytes and then waits for the "server". */
    var serverGate: CompletableDeferred<Unit>? = null
    var deleteResult: Result<MemberResponse>? = null
    var deleteCalls = 0

    override suspend fun updateMyProfile(
        phoneNumber: String?,
        birthDate: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
        identityStatus: String?,
        workOrSchool: String?,
    ): Result<MemberResponse> {
        lastUpdate = UpdateArgs(
            phoneNumber, birthDate, street, houseNumber, zipCode, city, identityStatus, workOrSchool,
        )
        // Mirror backend PATCH semantics: null keeps the old value.
        profile = profile.copy(
            phoneNumber = phoneNumber ?: profile.phoneNumber,
            birthDate = birthDate ?: profile.birthDate,
            street = street ?: profile.street,
            houseNumber = houseNumber ?: profile.houseNumber,
            zipCode = zipCode ?: profile.zipCode,
            city = city ?: profile.city,
            identityStatus = identityStatus ?: profile.identityStatus,
            workOrSchool = workOrSchool?.let { it.trim().ifBlank { null } } ?: profile.workOrSchool,
        )
        return Result.success(profile)
    }
    override suspend fun uploadProfileImage(
        image: PreparedProfileImage,
        onProgress: (sent: Long, total: Long?) -> Unit,
    ): Result<MemberResponse> {
        uploadedImages += image
        onProgress(image.byteLength / 2, image.byteLength)
        uploadGate?.await()
        onProgress(image.byteLength, image.byteLength)
        serverGate?.await()
        val result = uploadResults.removeFirstOrNull() ?: Result.success(
            profile.copy(
                profileImageUrl = "https://storage.example/u1.jpg?sig=${profile.profileImageVersion + 1}",
                profileImageVersion = profile.profileImageVersion + 1,
            ),
        )
        result.onSuccess { profile = it }
        return result
    }

    override suspend fun deleteProfileImage(): Result<MemberResponse> {
        deleteCalls++
        val result = deleteResult ?: Result.success(
            profile.copy(profileImageUrl = null, profileImageVersion = profile.profileImageVersion + 1),
        )
        result.onSuccess { profile = it }
        return result
    }
}

private class FakePickedImage : PickedProfileImage {
    var released = false
    override fun release() { released = true }
}

private class FakePreparedImage : PreparedProfileImage {
    var released = false
    override val byteLength: Long = 1000
    override val preview: Any = "preview"
    override fun open(): Source = Buffer()
    override fun release() { released = true }
}

private class FakePicker(var result: () -> ProfileImagePick) : ProfileImagePicker {
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun pick(): ProfileImagePick {
        gate?.await()
        return result()
    }
}

private class FakePreparer(var result: (PickedProfileImage) -> ProfileImagePreparation) : ProfileImagePreparer {
    var purged = 0
    override suspend fun prepare(picked: PickedProfileImage): ProfileImagePreparation = result(picked)
    override fun purgeStale() { purged++ }
}

private class FakeAvatarCache : ProfileAvatarCache {
    val evicted = mutableListOf<String>()
    override fun evict(cacheKey: String) { evicted += cacheKey }
}

private class OfflineException : Exception("Unable to resolve host")

private class FakeCityLookupRepository(private val cities: Map<String, String> = emptyMap()) : CityLookupRepository {
    override suspend fun cityForPostalCode(postalCode: String): String? = cities[postalCode]
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun vm(
        repo: MemberRepository,
        session: FakeConsentSession = FakeConsentSession(),
        cityLookup: CityLookupRepository = FakeCityLookupRepository(),
        preparer: FakePreparer = FakePreparer { ProfileImagePreparation.Ready(FakePreparedImage()) },
        avatarCache: FakeAvatarCache = FakeAvatarCache(),
    ) = ProfileViewModel(
        repo,
        session,
        FakeVerseRecordRepository(),
        cityLookup,
        preparer,
        avatarCache,
    )

    private fun success(viewModel: ProfileViewModel) =
        viewModel.uiState.value as ProfileUiState.Success

    @Test
    fun `load seeds edit house number from profile`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("12", success(viewModel).editHouseNumber)
    }

    @Test
    fun `update house number changes edit state`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateHouseNumber("12a")
        assertEquals("12a", success(viewModel).editHouseNumber)
    }

    @Test
    fun `save sends the edited house number to the repository`() = runTest {
        val repo = FakeMemberRepository()
        val viewModel = vm(repo)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateHouseNumber("12a")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("12a", repo.lastUpdate?.houseNumber)
        assertEquals("12a", success(viewModel).profile.houseNumber)
    }

    @Test
    fun `save sends null for an address that was never set`() = runTest {
        val repo = FakeMemberRepository()
        repo.profile = repo.profile.copy(street = null, houseNumber = null)
        val viewModel = vm(repo)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.lastUpdate?.street)
        assertNull(repo.lastUpdate?.houseNumber)
    }

    @Test
    fun `load seeds edit birth date in display format`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("1992.12.07", success(viewModel).editBirthDate)
    }

    @Test
    fun `state is not dirty after load and dirty after an edit`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, success(viewModel).isDirty)
        viewModel.updatePhone("+49 999")
        assertEquals(true, success(viewModel).isDirty)
    }

    @Test
    fun `save converts birth date to iso format`() = runTest {
        val repo = FakeMemberRepository()
        val viewModel = vm(repo)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateBirthDate("1990.01.31")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("1990-01-31", repo.lastUpdate?.birthDate)
    }

    @Test
    fun `save sets saveSuccess and state is clean again`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateCity("Bonn")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, success(viewModel).saveSuccess)
        assertEquals(false, success(viewModel).isDirty)
        viewModel.consumeSaveSuccess()
        assertEquals(false, success(viewModel).saveSuccess)
    }

    @Test
    fun `silent refresh does not clobber a dirty edit`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updatePhone("0151 111")
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("0151111", success(viewModel).editPhone)
    }

    private suspend fun loaded(
        repo: FakeMemberRepository = FakeMemberRepository(),
        cityLookup: CityLookupRepository = FakeCityLookupRepository(),
    ): ProfileViewModel {
        val viewModel = vm(repo, cityLookup = cityLookup)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()
        return viewModel
    }

    @Test
    fun `save is blocked and shows an error for a partial birth date`() = runTest {
        val repo = FakeMemberRepository()
        val viewModel = loaded(repo)
        viewModel.updateBirthDate("1992.1")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.lastUpdate)
        assertEquals(RegisterFieldError.DATE_INCOMPLETE, success(viewModel).birthDateError)
    }

    @Test
    fun `fields required at registration stay required`() = runTest {
        val repo = FakeMemberRepository()
        repo.profile = repo.profile.copy(identityStatus = null)
        val viewModel = loaded(repo)
        viewModel.updateBirthDate("")
        viewModel.updateZipCode("")
        viewModel.updateCity("")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.lastUpdate)
        val state = success(viewModel)
        assertEquals(RegisterFieldError.REQUIRED, state.birthDateError)
        assertEquals(RegisterFieldError.REQUIRED, state.identityStatusError)
        assertEquals(RegisterFieldError.REQUIRED, state.zipCodeError)
        assertEquals(RegisterFieldError.REQUIRED, state.cityError)
    }

    @Test
    fun `street and house number are required as a pair`() = runTest {
        val repo = FakeMemberRepository()
        val viewModel = loaded(repo)
        viewModel.updateHouseNumber("")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.lastUpdate)
        assertEquals(RegisterFieldError.REQUIRED, success(viewModel).houseNumberError)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val repo = FakeMemberRepository()
        repo.profile = repo.profile.copy(identityStatus = null)
        val viewModel = loaded(repo)
        viewModel.saveProfile()
        assertEquals(RegisterFieldError.REQUIRED, success(viewModel).identityStatusError)
        viewModel.updateIdentityStatus(IdentityStatus.UNIVERSITY_STUDENT)
        assertNull(success(viewModel).identityStatusError)
    }

    @Test
    fun `load seeds identity status work or school and the split phone number`() = runTest {
        val repo = FakeMemberRepository()
        repo.profile = repo.profile.copy(phoneNumber = "+821012345678", workOrSchool = "TU Berlin")
        val viewModel = loaded(repo)
        val state = success(viewModel)
        assertEquals(IdentityStatus.EMPLOYEE, state.editIdentityStatus)
        assertEquals("TU Berlin", state.editWorkOrSchool)
        assertEquals("KR", state.editPhoneCountryIso)
        assertEquals("1012345678", state.editPhone)
        assertEquals(false, state.isDirty)
    }

    @Test
    fun `save sends identity status work or school and the phone as E164`() = runTest {
        val repo = FakeMemberRepository()
        val viewModel = loaded(repo)
        viewModel.updateIdentityStatus(IdentityStatus.UNIVERSITY_STUDENT)
        viewModel.updateWorkOrSchool("  Siemens  ")
        viewModel.updatePhoneCountry("DE")
        viewModel.updatePhone("0151 2345678")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        val sent = repo.lastUpdate!!
        assertEquals("UNIVERSITY_STUDENT", sent.identityStatus)
        assertEquals("Siemens", sent.workOrSchool)
        assertEquals("+491512345678", sent.phoneNumber)
    }

    @Test
    fun `clearing stored optional fields sends empty strings`() = runTest {
        val repo = FakeMemberRepository()
        repo.profile = repo.profile.copy(phoneNumber = "+491512345678", workOrSchool = "TU Berlin")
        val viewModel = loaded(repo)
        viewModel.updatePhone("")
        viewModel.updateWorkOrSchool("")
        viewModel.updateStreet("")
        viewModel.updateHouseNumber("")
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
        val sent = repo.lastUpdate!!
        assertEquals("", sent.phoneNumber)
        assertEquals("", sent.workOrSchool)
        assertEquals("", sent.street)
        assertEquals("", sent.houseNumber)
    }

    @Test
    fun `a new postcode replaces the city loaded from the profile`() = runTest {
        val viewModel = loaded(cityLookup = FakeCityLookupRepository(mapOf("10115" to "Berlin")))
        viewModel.updateZipCode("10115")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Berlin", success(viewModel).editCity)
        assertEquals(false, success(viewModel).isCityLookupLoading)
    }

    @Test
    fun `a city typed by hand is not overwritten by the lookup`() = runTest {
        val viewModel = loaded(cityLookup = FakeCityLookupRepository(mapOf("10115" to "Berlin")))
        viewModel.updateCity("Potsdam")
        viewModel.updateZipCode("10115")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Potsdam", success(viewModel).editCity)
    }

    @Test
    fun `a failed lookup leaves the city alone`() = runTest {
        val viewModel = loaded()
        viewModel.updateZipCode("10115")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Köln", success(viewModel).editCity)
    }

    @Test
    fun `logout hands the sign out to the session`() = runTest {
        val session = FakeConsentSession()
        val viewModel = vm(FakeMemberRepository(), session = session)
        viewModel.logout()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, session.logoutCalls)
        assertEquals(false, session.forgotBiometric)
        assertEquals(true, viewModel.loggedOut.value)
    }

    // ─────────────────────────── photo (#139) ───────────────────────────

    private fun avatarVm(
        repo: FakeMemberRepository,
        preparer: FakePreparer = FakePreparer { ProfileImagePreparation.Ready(FakePreparedImage()) },
        cache: FakeAvatarCache = FakeAvatarCache(),
    ) = vm(repo, preparer = preparer, avatarCache = cache)

    private fun withPhoto(repo: FakeMemberRepository, version: Long = 2) {
        repo.profile = repo.profile.copy(profileImageUrl = "https://storage.example/u1.jpg?sig=old", profileImageVersion = version)
    }

    @Test
    fun cancellingThePickerChangesNothing() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Cancelled }); advanceUntilIdle()

        val state = success(viewModel)
        assertEquals(AvatarActivity.Idle, state.avatarActivity)
        assertEquals(2L, state.profile.profileImageVersion)
        assertTrue(repo.uploadedImages.isEmpty())
    }

    @Test
    fun anUnreadablePickAsksForAnotherPhoto() = runTest(dispatcher) {
        val viewModel = avatarVm(FakeMemberRepository())
        viewModel.loadProfile(); advanceUntilIdle()

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Unreadable }); advanceUntilIdle()

        assertEquals(AvatarActivity.Failed(ProfileImageFailure.UNREADABLE, canRetry = false), success(viewModel).avatarActivity)
    }

    @Test
    fun aSuccessfulUploadShowsOnlyTheServerAvatar() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val picked = FakePickedImage()
        val prepared = FakePreparedImage()
        val cache = FakeAvatarCache()
        val viewModel = avatarVm(repo, FakePreparer { ProfileImagePreparation.Ready(prepared) }, cache)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadGate = CompletableDeferred()

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(picked) }); advanceUntilIdle()

        // Half the bytes are out: uploading, unsaved preview, avatar still the old one.
        val during = success(viewModel)
        assertEquals(AvatarActivity.Uploading(500, 1000), during.avatarActivity)
        assertEquals(0.5f, (during.avatarActivity as AvatarActivity.Uploading).fraction)
        assertSame(prepared, during.avatarPreview)
        assertEquals(2L, during.avatar?.version)
        assertTrue(picked.released, "the original is freed once prepared")
        assertFalse(during.canChangeAvatar)

        repo.uploadGate!!.complete(Unit); advanceUntilIdle()

        val after = success(viewModel)
        assertEquals(AvatarActivity.Idle, after.avatarActivity)
        assertEquals(3L, after.avatar?.version)
        assertEquals("profile:u1:3", after.avatar?.cacheKey)
        assertEquals("https://storage.example/u1.jpg?sig=3", after.avatar?.url)
        assertNull(after.avatarPreview)
        assertTrue(prepared.released)
        assertEquals(listOf("profile:u1:2"), cache.evicted)
        assertNull(repo.lastUpdate, "the photo never goes through PATCH")
    }

    @Test
    fun allBytesSentIsStillNotSaved() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.serverGate = CompletableDeferred()

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        val state = success(viewModel)
        assertEquals(AvatarActivity.AwaitingServer, state.avatarActivity)
        assertNull(state.avatar, "nothing is shown as saved before the answer")
        repo.serverGate!!.complete(Unit); advanceUntilIdle()
        assertEquals(1L, success(viewModel).avatar?.version)
    }

    @Test
    fun aPreparationFailureNeverUploads() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val viewModel = avatarVm(repo, FakePreparer { ProfileImagePreparation.Failed(ProfileImageFailure.TOO_LARGE) })
        viewModel.loadProfile(); advanceUntilIdle()

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        assertEquals(AvatarActivity.Failed(ProfileImageFailure.TOO_LARGE, canRetry = false), success(viewModel).avatarActivity)
        assertTrue(repo.uploadedImages.isEmpty())
    }

    @Test
    fun aRetryableFailureKeepsTheFileAndRetrySendsItAgain() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val prepared = FakePreparedImage()
        val viewModel = avatarVm(repo, FakePreparer { ProfileImagePreparation.Ready(prepared) })
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadResults += Result.failure(ProfileImageException(ProfileImageFailure.STORAGE_UNAVAILABLE))

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        val failed = success(viewModel)
        assertEquals(AvatarActivity.Failed(ProfileImageFailure.STORAGE_UNAVAILABLE, canRetry = true), failed.avatarActivity)
        assertEquals(2L, failed.avatar?.version, "the old avatar stays")
        assertSame(prepared, failed.avatarPreview)
        assertFalse(prepared.released)

        viewModel.retryAvatarUpload(); advanceUntilIdle()

        assertEquals(listOf<PreparedProfileImage>(prepared, prepared), repo.uploadedImages)
        assertEquals(3L, success(viewModel).avatar?.version)
        assertTrue(prepared.released)
    }

    @Test
    fun aRejectedImageFreesTheFileAndOffersNoRetry() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val prepared = FakePreparedImage()
        val viewModel = avatarVm(repo, FakePreparer { ProfileImagePreparation.Ready(prepared) })
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadResults += Result.failure(ProfileImageException(ProfileImageFailure.INVALID_IMAGE))

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        assertEquals(AvatarActivity.Failed(ProfileImageFailure.INVALID_IMAGE, canRetry = false), success(viewModel).avatarActivity)
        assertTrue(prepared.released)
        viewModel.retryAvatarUpload(); advanceUntilIdle()
        assertEquals(1, repo.uploadedImages.size)
    }

    @Test
    fun aLostAnswerIsReconciledAndTheStoredPhotoShown() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadResults += Result.failure(ProfileImageException(ProfileImageFailure.NETWORK))
        // The server did store it; only the answer was lost.
        repo.profileResults += Result.success(
            repo.profile.copy(profileImageUrl = "https://storage.example/u1.jpg?sig=3", profileImageVersion = 3),
        )

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        val state = success(viewModel)
        assertEquals(AvatarActivity.Idle, state.avatarActivity)
        assertEquals(3L, state.avatar?.version)
    }

    @Test
    fun anUnansweredReconcileStaysUncertainUntilRechecked() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadResults += Result.failure(ProfileImageException(ProfileImageFailure.NETWORK))
        repo.profileResults += Result.failure(OfflineException())

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        val uncertain = success(viewModel)
        assertEquals(AvatarActivity.Uncertain, uncertain.avatarActivity)
        assertFalse(uncertain.canChangeAvatar, "no further mutation while the outcome is open")
        viewModel.retryAvatarUpload(); advanceUntilIdle()
        assertEquals(1, repo.uploadedImages.size)

        // Back online, the server still has version 2: it was not stored.
        viewModel.recheckAvatar(); advanceUntilIdle()

        val after = success(viewModel)
        assertEquals(AvatarActivity.Failed(ProfileImageFailure.NETWORK, canRetry = true), after.avatarActivity)
        assertEquals(2L, after.avatar?.version)
    }

    @Test
    fun cancellingDuringUploadAsksTheServerAndSendsNoDelete() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val prepared = FakePreparedImage()
        val viewModel = avatarVm(repo, FakePreparer { ProfileImagePreparation.Ready(prepared) })
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadGate = CompletableDeferred()
        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()
        val callsBefore = repo.profileCalls

        viewModel.cancelAvatarChange(); advanceUntilIdle()

        val state = success(viewModel)
        assertEquals(AvatarActivity.Idle, state.avatarActivity)
        assertEquals(callsBefore + 1, repo.profileCalls, "the outcome is checked with a GET")
        assertEquals(0, repo.deleteCalls)
        assertTrue(prepared.released)
        assertNull(state.avatarPreview)
    }

    @Test
    fun cancellingBeforeTheUploadStaysLocal() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val picker = FakePicker { ProfileImagePick.Selected(FakePickedImage()) }.apply { gate = CompletableDeferred() }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        viewModel.pickAvatar(picker); advanceUntilIdle()
        assertEquals(AvatarActivity.Picking, success(viewModel).avatarActivity)
        val callsBefore = repo.profileCalls

        viewModel.cancelAvatarChange()
        picker.gate!!.complete(Unit); advanceUntilIdle()

        assertEquals(AvatarActivity.Idle, success(viewModel).avatarActivity)
        assertEquals(callsBefore, repo.profileCalls)
        assertTrue(repo.uploadedImages.isEmpty())
    }

    @Test
    fun deletingNeedsConfirmationAndShowsInitialsAfterwards() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val cache = FakeAvatarCache()
        val viewModel = avatarVm(repo, cache = cache)
        viewModel.loadProfile(); advanceUntilIdle()

        viewModel.requestAvatarDelete()
        assertTrue(success(viewModel).showAvatarDeleteConfirm)
        viewModel.dismissAvatarDelete(); advanceUntilIdle()
        assertEquals(0, repo.deleteCalls)

        viewModel.requestAvatarDelete()
        viewModel.confirmAvatarDelete(); advanceUntilIdle()

        val state = success(viewModel)
        assertEquals(1, repo.deleteCalls)
        assertNull(state.avatar)
        assertFalse(state.showAvatarDeleteConfirm)
        assertEquals(AvatarActivity.Idle, state.avatarActivity)
        assertEquals(listOf("profile:u1:2"), cache.evicted)
    }

    @Test
    fun removeIsNotOfferedWithoutAPhoto() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()

        viewModel.requestAvatarDelete()
        viewModel.confirmAvatarDelete(); advanceUntilIdle()

        assertFalse(success(viewModel).showAvatarDeleteConfirm)
        assertEquals(0, repo.deleteCalls)
    }

    @Test
    fun aFailedDeleteKeepsThePhoto() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        repo.deleteResult = Result.failure(ProfileImageException(ProfileImageFailure.STORAGE_UNAVAILABLE))
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()

        viewModel.requestAvatarDelete()
        viewModel.confirmAvatarDelete(); advanceUntilIdle()

        val state = success(viewModel)
        assertEquals(AvatarActivity.Failed(ProfileImageFailure.STORAGE_UNAVAILABLE, canRetry = false), state.avatarActivity)
        assertEquals(2L, state.avatar?.version)
    }

    @Test
    fun aPhotoUploadKeepsTheUnsavedContactDraft() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        viewModel.startEditing()
        viewModel.updateCity("Bonn")

        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        val state = success(viewModel)
        assertEquals(1L, state.avatar?.version)
        assertEquals("Bonn", state.editCity)
        assertTrue(state.isEditing)
        assertTrue(state.isDirty)
        assertNull(repo.lastUpdate)
    }

    @Test
    fun cancellingTheFormKeepsAConfirmedPhoto() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        viewModel.startEditing()
        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        viewModel.cancelEditing()

        assertEquals(1L, success(viewModel).avatar?.version)
    }

    @Test
    fun theFormDoesNotSaveWhileThePhotoUploads() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        viewModel.startEditing()
        viewModel.updateCity("Bonn")
        repo.uploadGate = CompletableDeferred()
        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        viewModel.saveProfile(); advanceUntilIdle()

        assertNull(repo.lastUpdate)
        repo.uploadGate!!.complete(Unit); advanceUntilIdle()
        viewModel.saveProfile(); advanceUntilIdle()
        assertEquals("Bonn", repo.lastUpdate?.city)
        assertEquals(1L, success(viewModel).avatar?.version, "the PATCH answer does not undo the photo")
    }

    @Test
    fun aRefreshDuringUploadIsSkippedAndAnOlderVersionNeverWins() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it, version = 5) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadGate = CompletableDeferred()
        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()
        val callsBefore = repo.profileCalls

        viewModel.loadProfile(); advanceUntilIdle()
        assertEquals(callsBefore, repo.profileCalls)
        assertTrue(success(viewModel).avatarActivity is AvatarActivity.Uploading)

        repo.uploadGate!!.complete(Unit); advanceUntilIdle()
        // A stale GET (e.g. from a cache in between) still says version 5.
        repo.profileResults += Result.success(repo.profile.copy(profileImageUrl = "https://storage.example/stale", profileImageVersion = 5))
        viewModel.loadProfile(); advanceUntilIdle()

        assertEquals(6L, success(viewModel).avatar?.version)
    }

    @Test
    fun logoutDuringUploadFreesTheFile() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val prepared = FakePreparedImage()
        val preparer = FakePreparer { ProfileImagePreparation.Ready(prepared) }
        val viewModel = avatarVm(repo, preparer)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.uploadGate = CompletableDeferred()
        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()

        viewModel.logout(); advanceUntilIdle()

        assertTrue(prepared.released)
        assertNull(success(viewModel).avatarPreview)
        assertTrue(preparer.purged >= 2, "stale files are purged on start and on logout")
    }

    @Test
    fun anExpiredImageUrlIsRenewedOnceWithTheSameCacheKey() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        val expired = success(viewModel).avatar!!
        repo.profile = repo.profile.copy(profileImageUrl = "https://storage.example/u1.jpg?sig=fresh")
        val callsBefore = repo.profileCalls

        viewModel.onAvatarLoadFailed(expired, 403); advanceUntilIdle()
        viewModel.onAvatarLoadFailed(expired, 403); advanceUntilIdle()

        val renewed = success(viewModel).avatar!!
        assertEquals(callsBefore + 1, repo.profileCalls)
        assertEquals("https://storage.example/u1.jpg?sig=fresh", renewed.url)
        assertEquals(expired.cacheKey, renewed.cacheKey)
    }

    @Test
    fun otherImageErrorsDoNotRefresh() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        val callsBefore = repo.profileCalls

        viewModel.onAvatarLoadFailed(success(viewModel).avatar!!, null); advanceUntilIdle()

        assertEquals(callsBefore, repo.profileCalls)
    }

    @Test
    fun offlineKeepsTheCachedAvatarAndDisablesChanges() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.profileResults += Result.failure(OfflineException())

        viewModel.loadProfile(); advanceUntilIdle()

        val offline = success(viewModel)
        assertTrue(offline.isOffline)
        assertFalse(offline.canChangeAvatar)
        assertEquals(2L, offline.avatar?.version)
        viewModel.pickAvatar(FakePicker { ProfileImagePick.Selected(FakePickedImage()) }); advanceUntilIdle()
        assertTrue(repo.uploadedImages.isEmpty())

        viewModel.loadProfile(); advanceUntilIdle()
        assertFalse(success(viewModel).isOffline)
    }

    @Test
    fun aServerErrorOnRefreshIsNotOffline() = runTest(dispatcher) {
        val repo = FakeMemberRepository()
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        repo.profileResults += Result.failure(ProfileLoadException("Profile load failed (500)"))

        viewModel.loadProfile(); advanceUntilIdle()

        assertFalse(success(viewModel).isOffline)
    }

    @Test
    fun retryingTheConnectionWorksOverAnUnsavedDraft() = runTest(dispatcher) {
        val repo = FakeMemberRepository().also { withPhoto(it) }
        val viewModel = avatarVm(repo)
        viewModel.loadProfile(); advanceUntilIdle()
        viewModel.startEditing()
        viewModel.updateCity("Bonn")
        repo.profileResults += Result.failure(OfflineException())
        viewModel.retryConnection(); advanceUntilIdle()
        assertTrue(success(viewModel).isOffline)

        viewModel.retryConnection(); advanceUntilIdle()

        val state = success(viewModel)
        assertFalse(state.isOffline)
        assertTrue(state.canChangeAvatar)
        assertEquals("Bonn", state.editCity)
    }
}
