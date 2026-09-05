package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.login.domain.repository.CityLookupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuthRepository : AuthRepository {
    var registerCalls = 0
    var lastRequest: RegisterRequest? = null
    var registerResult: Result<Unit> = Result.success(Unit)
    var loginCalls = 0
    var lastLogin: Pair<String, String>? = null
    /** null = succeed; set a throwable to make the auto-login fail. */
    var loginFailure: Throwable? = null

    override suspend fun login(user: String, pass: String): TokenResponse {
        loginCalls++
        lastLogin = user to pass
        loginFailure?.let { throw it }
        return TokenResponse(accessToken = "at", expiresIn = 300, refreshToken = "rt", tokenType = "Bearer")
    }

    override suspend fun register(request: RegisterRequest): Result<Unit> {
        registerCalls++
        lastRequest = request
        return registerResult
    }
}

private class FakeCityLookupRepository : CityLookupRepository {
    override suspend fun cityForPostalCode(postalCode: String): String? = null
}

private class FakeMemberRepository : MemberRepository {
    var status: MemberStatus = MemberStatus.PENDING
    var profileFailure: Throwable? = null
    override suspend fun getMyProfile(): Result<MemberResponse> =
        profileFailure?.let { Result.failure(it) }
            ?: Result.success(MemberResponse(publicId = "p1", firstName = "승진", lastName = "김", status = status))

    override suspend fun updateMyProfile(
        phoneNumber: String?, profileImageUrl: String?, birthDate: String?,
        street: String?, houseNumber: String?, zipCode: String?, city: String?,
    ): Result<MemberResponse> = getMyProfile()
}

private class FakeTokenStorage : TokenStorage {
    private var access: String? = null
    private var refresh: String? = null
    var keptSignedIn = false
    override fun saveAccessToken(token: String) { access = token }
    override fun getAccessToken(): String? = access
    override fun saveRefreshToken(token: String?) { refresh = token }
    override fun getRefreshToken(): String? = refresh
    override fun clear() { access = null; refresh = null }
    override fun setKeepSignedIn(value: Boolean) { keptSignedIn = value }
    override fun isKeepSignedIn(): Boolean = keptSignedIn
    override fun setBiometricEnabled(value: Boolean) = Unit
    override fun isBiometricEnabled(): Boolean = false
}

/**
 * The bug this guards against: the v2 screen had no house number field, so
 * filling in the street alone produced a REQUIRED error on a field nobody
 * rendered. Submit returned before the HTTP call with nothing visible
 * changing, and the button looked dead (#155).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var auth: FakeAuthRepository
    private lateinit var members: FakeMemberRepository
    private lateinit var tokens: FakeTokenStorage
    private lateinit var vm: RegisterViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        auth = FakeAuthRepository()
        members = FakeMemberRepository()
        tokens = FakeTokenStorage()
        // A bare client is enough: invalidateBearerCache is a no-op without the
        // Auth plugin installed, and no test here makes an HTTP call.
        vm = RegisterViewModel(auth, tokens, FakeCityLookupRepository(), members, HttpClient(MockEngine { respondOk() }))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** Everything the validator demands, with no address at all. */
    private fun fillMinimumValidForm() {
        vm.onLastNameChange("김")
        vm.onFirstNameChange("승진")
        vm.onEmailChange("hello@hanmaum.de")
        vm.onPasswordChange("Passwort1!")
        vm.onZipChange("40210")
        vm.onCityChange("Düsseldorf")
    }

    @Test
    fun aStreetWithoutAHouseNumberBlocksSubmitAndSaysSo() = runTest {
        fillMinimumValidForm()
        vm.onStreetChange("Musterstraße")

        vm.register()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(0, auth.registerCalls, "must not reach the backend")
        assertEquals(RegisterFieldError.REQUIRED, s.houseNumberError, "the house number is what is missing")
        assertEquals(RegisterField.HOUSE_NUMBER, s.focusTarget, "and the form must point at it")
        assertNotNull(s.bannerError, "a blocked submit must say why at the top of the form")
    }

    @Test
    fun aHouseNumberWithoutAStreetBlocksSubmitToo() = runTest {
        fillMinimumValidForm()
        vm.onHouseNumberChange("12a")

        vm.register()
        advanceUntilIdle()

        assertEquals(0, auth.registerCalls)
        assertEquals(RegisterFieldError.REQUIRED, vm.uiState.value.streetError)
    }

    @Test
    fun aCompleteAddressSubmits() = runTest {
        fillMinimumValidForm()
        vm.onStreetChange("Musterstraße")
        vm.onHouseNumberChange("12a")

        vm.register()
        advanceUntilIdle()

        assertEquals(1, auth.registerCalls)
        assertEquals("Musterstraße", auth.lastRequest?.street)
        assertEquals("12a", auth.lastRequest?.houseNumber)
    }

    @Test
    fun noAddressAtAllIsFine() = runTest {
        // The address is optional as a whole — only the split must be consistent.
        fillMinimumValidForm()

        vm.register()
        advanceUntilIdle()

        assertEquals(1, auth.registerCalls)
        assertNull(auth.lastRequest?.street)
        assertNull(auth.lastRequest?.houseNumber)
    }

    @Test
    fun everyMissingRequiredFieldIsReportedAtOnce() = runTest {
        // Not one at a time: the user should see the whole list after one tap.
        vm.register()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(0, auth.registerCalls)
        assertEquals(RegisterFieldError.REQUIRED, s.lastNameError)
        assertEquals(RegisterFieldError.REQUIRED, s.firstNameError)
        assertEquals(RegisterFieldError.REQUIRED, s.emailError)
        assertEquals(RegisterFieldError.REQUIRED, s.passwordError)
        assertEquals(RegisterFieldError.REQUIRED, s.zipCodeError)
        assertEquals(RegisterFieldError.REQUIRED, s.cityError)
        assertEquals(RegisterBanner.MissingRequired, s.bannerError)
        assertEquals(RegisterField.LAST_NAME, s.focusTarget, "focus goes to the first field in visual order")
    }

    @Test
    fun aWeakPasswordBlocksSubmit() = runTest {
        fillMinimumValidForm()
        vm.onPasswordChange("passwort")

        vm.register()
        advanceUntilIdle()

        assertEquals(0, auth.registerCalls)
        assertEquals(RegisterFieldError.PASSWORD_REQUIREMENTS, vm.uiState.value.passwordError)
    }

    @Test
    fun thePasswordChecklistTracksTyping() = runTest {
        vm.onEmailChange("hello@hanmaum.de")

        vm.onPasswordChange("passwort")
        vm.uiState.value.passwordCriteria.let {
            assertTrue(it.minLength)
            assertTrue(!it.hasUpperAndLower)
            assertTrue(!it.hasDigit)
            assertTrue(!it.hasSpecial)
        }

        vm.onPasswordChange("Passwort1!")
        assertTrue(vm.uiState.value.passwordCriteria.allMet)
    }

    @Test
    fun anIncompleteBirthDateBlocksSubmit() = runTest {
        fillMinimumValidForm()
        vm.onBirthDateChange("2000.08")

        vm.register()
        advanceUntilIdle()

        assertEquals(0, auth.registerCalls)
        assertEquals(RegisterFieldError.DATE_INCOMPLETE, vm.uiState.value.birthDateError)
    }

    @Test
    fun aCompleteBirthDateReachesTheBackendAsIsoDate() = runTest {
        fillMinimumValidForm()
        vm.onBirthDateChange("2000.08.16")

        vm.register()
        advanceUntilIdle()

        assertEquals(1, auth.registerCalls)
        assertEquals("2000-08-16", auth.lastRequest?.birthDate)
    }

    @Test
    fun editingAFieldClearsItsErrorAndTheBanner() = runTest {
        vm.register()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.bannerError)

        vm.onLastNameChange("김")

        assertNull(vm.uiState.value.lastNameError)
        assertNull(vm.uiState.value.bannerError)
    }

    // ── auto-login after registration (#168) ─────────────────────────────

    @Test
    fun aSuccessfulRegistrationSignsTheMemberInWithTheSameCredentials() = runTest {
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        assertEquals(1, auth.loginCalls, "registration must be followed by a login")
        assertEquals("hello@hanmaum.de" to "Passwort1!", auth.lastLogin)
        assertTrue(tokens.keptSignedIn, "the session must survive the next app start")
    }

    @Test
    fun aFreshRegistrationLandsOnThePendingScreen() = runTest {
        // The bug: this used to stop at a banner because the auto-login threw
        // and nothing said why.
        members.status = MemberStatus.PENDING
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        assertEquals(NavRoute.PendingApproval, vm.uiState.value.navigateTo)
        assertNull(vm.uiState.value.bannerError, "a working auto-login shows no banner")
    }

    @Test
    fun anAlreadyActiveAccountGoesHomeInsteadOfPending() = runTest {
        // Registering onto an existing active account must not send the member
        // to a screen telling them to wait for an approval they already have.
        members.status = MemberStatus.ACTIVE
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
    }

    @Test
    fun aRefusedAccountGoesToTheRejectedScreen() = runTest {
        members.status = MemberStatus.REJECTED
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        assertEquals(NavRoute.Rejected, vm.uiState.value.navigateTo)
    }

    @Test
    fun aFailedProfileCallStillLandsOnPendingRatherThanTheLoginForm() = runTest {
        // The token is valid and the account exists — only the profile fetch
        // failed. Sending them back to log in again would be a regression.
        members.profileFailure = IllegalStateException("offline")
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        assertEquals(NavRoute.PendingApproval, vm.uiState.value.navigateTo)
    }

    @Test
    fun aFailedAutoLoginKeepsRegistrationASuccessAndAsksForLogin() = runTest {
        // The fallback stays: the account was created, so this must never read
        // as a failed registration.
        auth.loginFailure = IllegalStateException("401 from keycloak")
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(1, auth.registerCalls)
        assertEquals(RegisterBanner.RegisteredPleaseLogin, s.bannerError)
        assertTrue(s.isSuccess, "registration itself succeeded")
        assertNull(s.navigateTo, "and no navigation happens")
    }

    @Test
    fun aBlockedSubmitNeverReachesTheLogin() = runTest {
        vm.register()
        advanceUntilIdle()

        assertEquals(0, auth.registerCalls)
        assertEquals(0, auth.loginCalls)
    }
}
