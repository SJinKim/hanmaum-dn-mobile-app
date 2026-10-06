package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterException
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuthRepository : AuthRepository {
    var registerCalls = 0
    var lastRequest: RegisterRequest? = null
    var registerResult: Result<Unit> = Result.success(Unit)
    var loginCalls = 0
    var lastLogin: Pair<String, String>? = null

    override suspend fun exchangeAuthorizationCode(code: String, verifier: String): TokenResponse {
        loginCalls++
        lastLogin = code to verifier
        return TokenResponse(accessToken = "at", expiresIn = 300, refreshToken = "rt", tokenType = "Bearer")
    }

    override suspend fun refresh(refreshToken: String): TokenResponse =
        exchangeAuthorizationCode("", "")

    override suspend fun register(request: RegisterRequest): Result<Unit> {
        registerCalls++
        lastRequest = request
        return registerResult
    }
}

private class FakeCityLookupRepository : CityLookupRepository {
    override suspend fun cityForPostalCode(postalCode: String): String? = null
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
    private lateinit var vm: RegisterViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        auth = FakeAuthRepository()
        vm = RegisterViewModel(auth, FakeCityLookupRepository())
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

    // Registration hands over to browser login (#268), never a password grant.
    @Test
    fun successfulRegistrationOpensLoginWithVerificationNoticeWithoutTokenExchange() = runTest {
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(1, auth.registerCalls)
        assertEquals(0, auth.loginCalls)
        assertNull(auth.lastLogin)
        assertTrue(state.isSuccess)
        assertEquals(NavRoute.Login, state.navigateTo)
        assertEquals(LoginRoute.NOTICE_VERIFY_EMAIL, state.loginNotice)
        assertEquals("", state.password)
        assertNull(state.bannerError)
    }

    @Test
    fun aBlockedSubmitNeverReachesTheLogin() = runTest {
        vm.register()
        advanceUntilIdle()
        assertEquals(0, auth.registerCalls)
        assertEquals(0, auth.loginCalls)
    }

    // ── birth date normalised at submit (#166) ───────────────────────────

    @Test
    fun sevenDigitsAreNormalisedWhenSubmittingWithoutLeavingTheField() = runTest {
        fillMinimumValidForm()
        vm.onBirthDateChange("2000.22.3") // what typing 2000223 leaves behind

        vm.register()
        advanceUntilIdle()

        assertEquals("2000.02.23", vm.uiState.value.birthDate)
        assertEquals(1, auth.registerCalls)
        assertEquals("2000-02-23", auth.lastRequest?.birthDate)
    }

    @Test
    fun anAmbiguousBirthDateBlocksSubmitInsteadOfGuessing() = runTest {
        // 2000105 is either 5 January or 10 May; sending either would be a
        // date the member never typed.
        fillMinimumValidForm()
        vm.onBirthDateChange("2000.10.5")

        vm.register()
        advanceUntilIdle()

        assertEquals(0, auth.registerCalls, "no request on an ambiguous date")
        assertEquals(RegisterFieldError.DATE_INCOMPLETE, vm.uiState.value.birthDateError)
    }

    @Test
    fun animpossibleDateBlocksSubmit() = runTest {
        fillMinimumValidForm()
        vm.onBirthDateChange("2000.02.30")

        vm.register()
        advanceUntilIdle()

        assertEquals(0, auth.registerCalls)
        assertEquals(RegisterFieldError.DATE_INVALID, vm.uiState.value.birthDateError)
    }

    @Test
    fun aServerFailureOpensTheUnavailableDialogAndKeepsTheForm() = runTest {
        // A 5xx is nothing the member can fix by editing the form (#156).
        auth.registerResult = Result.failure(RegisterException("Internal error", isServerError = true))
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.showUnavailableDialog)
        assertNull(s.bannerError, "the dialog replaces the banner, it does not stack on it")
        assertFalse(s.isLoading)
        assertEquals("hello@hanmaum.de", s.email, "what was typed survives the failure")
        assertEquals(0, auth.loginCalls)
    }

    @Test
    fun aRefusalWithAMessageStaysABannerNotADialog() = runTest {
        auth.registerResult = Result.failure(RegisterException("Email already registered"))
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.showUnavailableDialog)
        assertEquals(RegisterBanner.ServerMessage("Email already registered"), s.bannerError)
    }

    @Test
    fun dismissingTheUnavailableDialogClosesIt() = runTest {
        auth.registerResult = Result.failure(RegisterException(null, isServerError = true))
        fillMinimumValidForm()
        vm.register()
        advanceUntilIdle()

        vm.onUnavailableDialogDismissed()

        assertFalse(vm.uiState.value.showUnavailableDialog)
    }
}
