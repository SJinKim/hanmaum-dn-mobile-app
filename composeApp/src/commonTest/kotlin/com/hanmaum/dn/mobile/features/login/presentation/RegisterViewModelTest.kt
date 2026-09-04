package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
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

    override suspend fun login(user: String, pass: String): TokenResponse =
        throw IllegalStateException("login is not part of these tests")

    override suspend fun register(request: RegisterRequest): Result<Unit> {
        registerCalls++
        lastRequest = request
        return registerResult
    }
}

private class FakeCityLookupRepository : CityLookupRepository {
    override suspend fun cityForPostalCode(postalCode: String): String? = null
}

private class FakeTokenStorage : TokenStorage {
    private var access: String? = null
    private var refresh: String? = null
    override fun saveAccessToken(token: String) { access = token }
    override fun getAccessToken(): String? = access
    override fun saveRefreshToken(token: String?) { refresh = token }
    override fun getRefreshToken(): String? = refresh
    override fun clear() { access = null; refresh = null }
    override fun setKeepSignedIn(value: Boolean) = Unit
    override fun isKeepSignedIn(): Boolean = true
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
    private lateinit var vm: RegisterViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        auth = FakeAuthRepository()
        vm = RegisterViewModel(auth, FakeTokenStorage(), FakeCityLookupRepository())
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
}
