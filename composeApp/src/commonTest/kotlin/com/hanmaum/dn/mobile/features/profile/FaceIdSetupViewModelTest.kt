package com.hanmaum.dn.mobile.features.profile

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import com.hanmaum.dn.mobile.features.login.domain.model.LoginException
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.profile.presentation.FaceIdSetupViewModel
import com.russhwolf.settings.MapSettings
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

private class SetupAuthRepository(private val correctPassword: String?) : AuthRepository {
    var loginCalls = 0
    override suspend fun login(user: String, pass: String): TokenResponse {
        loginCalls++
        if (pass != correctPassword) {
            throw LoginException(status = 401, error = "invalid_grant", description = "Invalid user credentials")
        }
        return TokenResponse(accessToken = "a", expiresIn = 300, refreshToken = "r", tokenType = "Bearer")
    }
    override suspend fun register(request: RegisterRequest): Result<Unit> = Result.success(Unit)
}

private class SetupMemberRepository(private val email: String?) : MemberRepository {
    override suspend fun getMyProfile(): Result<MemberResponse> = Result.success(
        MemberResponse(
            publicId = "p1", firstName = "서진", lastName = "김",
            email = email, status = MemberStatus.ACTIVE,
        ),
    )
    override suspend fun updateMyProfile(
        phoneNumber: String?, profileImageUrl: String?, birthDate: String?,
        street: String?, houseNumber: String?, zipCode: String?, city: String?,
    ): Result<MemberResponse> = getMyProfile()
}

private class SetupSecureStore : SecureStore {
    private val values = mutableMapOf<String, String>()
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getString(key: String): String? = values[key]
    override fun remove(key: String) { values.remove(key) }
}

/**
 * Switching Face ID on from 설정. The switch alone could never arm it: the
 * screen is reachable only while signed in, so the password it has to replay
 * was never around to store (#197).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FaceIdSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val settings = MapSettings()
    private val authPreferences = AuthPreferencesImpl(settings)
    private val credentialStore = CredentialStore(SetupSecureStore())

    private fun vm(
        correctPassword: String? = "pw",
        email: String? = "seojin@hanmaum.de",
        auth: SetupAuthRepository = SetupAuthRepository(correctPassword),
    ) = FaceIdSetupViewModel(auth, SetupMemberRepository(email), credentialStore, authPreferences)

    @Test
    fun theRightPasswordStoresTheCredentialsAndArmsFaceId() = runTest(dispatcher) {
        val viewModel = vm()
        viewModel.onToggle(true)
        assertTrue(viewModel.uiState.value.askingForPassword)

        viewModel.onPasswordChange("pw")
        viewModel.onConfirm()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.enabled)
        assertFalse(viewModel.uiState.value.askingForPassword)
        assertTrue(authPreferences.isBiometricEnabled())
        assertEquals("seojin@hanmaum.de", credentialStore.getCredentials()?.email)
        assertEquals("pw", credentialStore.getCredentials()?.password)
    }

    @Test
    fun aWrongPasswordChangesNothing() = runTest(dispatcher) {
        val viewModel = vm()
        viewModel.onToggle(true)
        viewModel.onPasswordChange("nope")
        viewModel.onConfirm()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.askingForPassword, "the sheet stays open to retry")
        assertFalse(viewModel.uiState.value.enabled)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(credentialStore.getCredentials())
    }

    @Test
    fun theSheetIsNotAskedToVerifyAnEmptyPassword() = runTest(dispatcher) {
        val auth = SetupAuthRepository("pw")
        val viewModel = vm(auth = auth)
        viewModel.onToggle(true)
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(0, auth.loginCalls)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun switchingItOffForgetsTheStoredPassword() = runTest(dispatcher) {
        val viewModel = vm()
        viewModel.onToggle(true)
        viewModel.onPasswordChange("pw")
        viewModel.onConfirm()
        advanceUntilIdle()

        viewModel.onToggle(false)

        assertFalse(viewModel.uiState.value.enabled)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(credentialStore.getCredentials())
    }

    @Test
    fun backingOutOfTheSheetStoresNothing() = runTest(dispatcher) {
        val viewModel = vm()
        viewModel.onToggle(true)
        viewModel.onPasswordChange("pw")
        viewModel.onDismiss()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.askingForPassword)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(credentialStore.getCredentials())
    }

    @Test
    fun anAccountWithoutAnEmailCannotArmFaceId() = runTest(dispatcher) {
        // Face ID replays an email and a password; without the address there is
        // nothing to store that would ever sign anyone in.
        val viewModel = vm(email = null)
        viewModel.onToggle(true)
        viewModel.onPasswordChange("pw")
        viewModel.onConfirm()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(credentialStore.getCredentials())
    }
}
