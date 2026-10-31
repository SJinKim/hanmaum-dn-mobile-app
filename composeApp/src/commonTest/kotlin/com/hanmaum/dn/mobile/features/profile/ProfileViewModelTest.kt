package com.hanmaum.dn.mobile.features.profile

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.features.consent.FakeConsentSession
import com.hanmaum.dn.mobile.features.login.domain.model.IdentityStatus
import com.hanmaum.dn.mobile.features.login.domain.repository.CityLookupRepository
import com.hanmaum.dn.mobile.features.login.presentation.RegisterFieldError
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
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
        val profileImageUrl: String?,
        val birthDate: String?,
        val street: String?,
        val houseNumber: String?,
        val zipCode: String?,
        val city: String?,
        val identityStatus: String?,
        val workOrSchool: String?,
    )

    var lastUpdate: UpdateArgs? = null

    override suspend fun getMyProfile(): Result<MemberResponse> = Result.success(profile)

    override suspend fun updateMyProfile(
        phoneNumber: String?,
        profileImageUrl: String?,
        birthDate: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
        identityStatus: String?,
        workOrSchool: String?,
    ): Result<MemberResponse> {
        lastUpdate = UpdateArgs(
            phoneNumber, profileImageUrl, birthDate, street, houseNumber, zipCode, city, identityStatus, workOrSchool,
        )
        // Mirror backend PATCH semantics: null keeps the old value.
        profile = profile.copy(
            phoneNumber = phoneNumber ?: profile.phoneNumber,
            profileImageUrl = profileImageUrl ?: profile.profileImageUrl,
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
}

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
    ) = ProfileViewModel(
        repo,
        session,
        FakeVerseRecordRepository(),
        cityLookup,
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
}
