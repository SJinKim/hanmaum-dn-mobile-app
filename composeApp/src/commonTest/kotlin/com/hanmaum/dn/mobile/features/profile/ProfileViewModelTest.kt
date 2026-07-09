package com.hanmaum.dn.mobile.features.profile

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
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
    )

    data class UpdateArgs(
        val phoneNumber: String?,
        val profileImageUrl: String?,
        val birthDate: String?,
        val street: String?,
        val houseNumber: String?,
        val zipCode: String?,
        val city: String?,
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
    ): Result<MemberResponse> {
        lastUpdate = UpdateArgs(phoneNumber, profileImageUrl, birthDate, street, houseNumber, zipCode, city)
        // Mirror backend PATCH semantics: null keeps the old value.
        profile = profile.copy(
            phoneNumber = phoneNumber ?: profile.phoneNumber,
            profileImageUrl = profileImageUrl ?: profile.profileImageUrl,
            birthDate = birthDate ?: profile.birthDate,
            street = street ?: profile.street,
            houseNumber = houseNumber ?: profile.houseNumber,
            zipCode = zipCode ?: profile.zipCode,
            city = city ?: profile.city,
        )
        return Result.success(profile)
    }
}

private class FakeTokenStorage : TokenStorage {
    private var access: String? = null
    private var refresh: String? = null
    private var keepSignedIn = true
    private var biometric = false
    override fun saveAccessToken(token: String) { access = token }
    override fun getAccessToken(): String? = access
    override fun saveRefreshToken(token: String?) { refresh = token }
    override fun getRefreshToken(): String? = refresh
    override fun clear() { access = null; refresh = null }
    override fun setKeepSignedIn(value: Boolean) { keepSignedIn = value }
    override fun isKeepSignedIn(): Boolean = keepSignedIn
    override fun setBiometricEnabled(value: Boolean) { biometric = value }
    override fun isBiometricEnabled(): Boolean = biometric
}

private class InMemorySecureStore : SecureStore {
    private val map = mutableMapOf<String, String>()
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String): String? = map[key]
    override fun remove(key: String) { map.remove(key) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun vm(repo: MemberRepository) =
        ProfileViewModel(repo, FakeTokenStorage(), CredentialStore(InMemorySecureStore()))

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
    fun `save sends null when house number is blank`() = runTest {
        val repo = FakeMemberRepository()
        repo.profile = repo.profile.copy(houseNumber = null)
        val viewModel = vm(repo)
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()
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
        viewModel.updatePhone("+49 111")
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("+49 111", success(viewModel).editPhone)
    }

    @Test
    fun `partial birth date is invalid full or empty birth date is valid`() = runTest {
        val viewModel = vm(FakeMemberRepository())
        viewModel.loadProfile()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateBirthDate("1992.1")
        assertEquals(false, success(viewModel).isBirthDateValid)
        viewModel.updateBirthDate("1992.01.07")
        assertEquals(true, success(viewModel).isBirthDateValid)
        viewModel.updateBirthDate("")
        assertEquals(true, success(viewModel).isBirthDateValid)
    }
}
