package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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
import kotlin.test.assertTrue

private class FaceIdAuthRepository : AuthRepository {
    override suspend fun login(user: String, pass: String) = TokenResponse(
        accessToken = "access", expiresIn = 300, refreshToken = "refresh", tokenType = "Bearer",
    )
    override suspend fun register(request: RegisterRequest): Result<Unit> = Result.success(Unit)
}

private class FaceIdMemberRepository : MemberRepository {
    override suspend fun getMyProfile(): Result<MemberResponse> = Result.success(
        MemberResponse(publicId = "p1", firstName = "서진", lastName = "김", status = MemberStatus.ACTIVE),
    )
    override suspend fun updateMyProfile(
        phoneNumber: String?, profileImageUrl: String?, birthDate: String?,
        street: String?, houseNumber: String?, zipCode: String?, city: String?,
    ): Result<MemberResponse> = getMyProfile()
}

private class InMemorySecureStore : SecureStore {
    private val values = mutableMapOf<String, String>()
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getString(key: String): String? = values[key]
    override fun remove(key: String) { values.remove(key) }
}

/**
 * Face ID sign-in, driven the way a member drives it: switch it on in 설정,
 * sign in once with the password, and it is armed for the next launch.
 *
 * It regressed because 설정 wrote AuthPreferences while the login path read a
 * second, identically-meaning flag on TokenStorage — so the toggle steered
 * nothing and the credentials were never stored.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelFaceIdTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val settings = MapSettings()
    private val authPreferences = AuthPreferencesImpl(settings)
    private val credentialStore = CredentialStore(InMemorySecureStore())

    private fun viewModel() = LoginViewModel(
        authRepository = FaceIdAuthRepository(),
        memberRepository = FaceIdMemberRepository(),
        tokenStorage = TokenStorageImpl(settings),
        httpClient = HttpClient(MockEngine { respond("") }),
        credentialStore = credentialStore,
        authPreferences = authPreferences,
    )

    @Test
    fun signingInWithFaceIdEnabledInSettingsArmsTheNextLaunch() = runTest(dispatcher) {
        // what the 설정 screen does when Face ID 로그인 is switched on
        authPreferences.setBiometricEnabled(true)

        viewModel().onLoginClicked("seojin@hanmaum.de", "pw")
        advanceUntilIdle()

        assertTrue(viewModel().canFaceIdSignIn())
        assertEquals("seojin@hanmaum.de", viewModel().savedCredentials()?.email)
        assertEquals("pw", viewModel().savedCredentials()?.password)
    }

    @Test
    fun signingInWithFaceIdOffStoresNothing() = runTest(dispatcher) {
        viewModel().onLoginClicked("seojin@hanmaum.de", "pw")
        advanceUntilIdle()

        assertFalse(viewModel().canFaceIdSignIn())
    }

    @Test
    fun keepSignedInChoiceLandsWhereTheSplashReadsIt() = runTest(dispatcher) {
        viewModel().onLoginClicked("seojin@hanmaum.de", "pw", keepSignedIn = false)
        advanceUntilIdle()

        assertFalse(authPreferences.isKeepSignedInEnabled())
    }
}
