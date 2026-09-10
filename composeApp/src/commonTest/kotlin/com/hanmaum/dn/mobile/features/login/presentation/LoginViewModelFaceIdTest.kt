package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.security.FakeBiometricVault
import com.hanmaum.dn.mobile.core.security.VaultResult
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FaceIdAuthRepository(private val refreshWorks: Boolean = true) : AuthRepository {
    var refreshedWith: String? = null
    override suspend fun login(user: String, pass: String) = TokenResponse(
        accessToken = "access", expiresIn = 300, refreshToken = "refresh", tokenType = "Bearer",
    )
    override suspend fun refresh(refreshToken: String): TokenResponse {
        refreshedWith = refreshToken
        if (!refreshWorks) throw IllegalStateException("refresh token expired")
        return TokenResponse(
            accessToken = "fresh-access", expiresIn = 300,
            refreshToken = "rotated-refresh", tokenType = "Bearer",
        )
    }
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

/**
 * Signing in with Face ID: the vault releases the refresh token, and that token
 * buys the session. No password is stored, so none can be replayed (#200).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelFaceIdTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val settings = MapSettings()
    private val authPreferences = AuthPreferencesImpl(settings)
    private val tokenStorage = TokenStorageImpl(settings)
    private val vault = FakeBiometricVault()

    private fun viewModel(auth: AuthRepository = FaceIdAuthRepository()) = LoginViewModel(
        authRepository = auth,
        memberRepository = FaceIdMemberRepository(),
        tokenStorage = tokenStorage,
        httpClient = HttpClient(MockEngine { respond("") }),
        authPreferences = authPreferences,
    )

    private suspend fun signIn(vm: LoginViewModel) =
        vm.signInWithFaceId(vault, "title", "subtitle", "cancel")

    /** What the 설정 switch leaves behind once Face ID is on. */
    private suspend fun armed() {
        authPreferences.setBiometricEnabled(true)
        vault.seal("sealed-refresh", "t", "s", "c")
    }

    @Test
    fun anArmedVaultSignsTheMemberIn() = runTest(dispatcher) {
        armed()
        val auth = FaceIdAuthRepository()
        val vm = viewModel(auth)

        signIn(vm)
        advanceUntilIdle()

        assertEquals("sealed-refresh", auth.refreshedWith)
        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
        assertEquals("fresh-access", tokenStorage.getAccessToken())
        assertEquals("rotated-refresh", tokenStorage.getRefreshToken(), "the token rotates on use")
    }

    @Test
    fun theRotatedTokenGoesBackIntoTheVault() = runTest(dispatcher) {
        // Without this the vault keeps the token that was just spent, and the
        // second sign-in replays a dead one — Face ID works exactly once (#212).
        armed()
        val vm = viewModel()

        signIn(vm)
        advanceUntilIdle()

        assertEquals("rotated-refresh", vault.sealed)
        assertEquals(1, vault.resealCount, "and it costs no second prompt")
    }

    @Test
    fun twoSignInsInARowBothWork() = runTest(dispatcher) {
        armed()
        val auth = FaceIdAuthRepository()

        signIn(viewModel(auth))
        advanceUntilIdle()
        val second = viewModel(auth)
        signIn(second)
        advanceUntilIdle()

        assertEquals("rotated-refresh", auth.refreshedWith, "the second one used the rotated token")
        assertEquals(NavRoute.Home, second.uiState.value.navigateTo)
    }

    @Test
    fun aBiometricLockoutDoesNotDisarmTheSwitch() = runTest(dispatcher) {
        // A lockout after failed attempts reports "unavailable" on both platforms.
        // Treating that as "never set up" made a passing condition permanent: the
        // button was gone until Face ID was set up again (#212).
        armed()
        vault.available = false
        val vm = viewModel()

        signIn(vm)
        advanceUntilIdle()

        assertTrue(authPreferences.isBiometricEnabled(), "it is unavailable now, not gone")
        assertNotNull(vault.sealed)
    }

    @Test
    fun anEmptyVaultDoesDisarmTheSwitch() = runTest(dispatcher) {
        // The other half of the pair: nothing sealed is permanent, not passing.
        authPreferences.setBiometricEnabled(true)
        val vm = viewModel()

        signIn(vm)
        advanceUntilIdle()

        assertFalse(authPreferences.isBiometricEnabled())
    }

    @Test
    fun faceIdIsOfferedOnlyWhenSwitchedOnAndSealed() = runTest(dispatcher) {
        val vm = viewModel()
        assertFalse(vm.canFaceIdSignIn(vault), "nothing sealed yet")

        armed()

        assertTrue(vm.canFaceIdSignIn(vault))
    }

    @Test
    fun cancellingThePromptSaysNothingAndSignsNobodyIn() = runTest(dispatcher) {
        armed()
        vault.nextResult = VaultResult.Cancelled
        val vm = viewModel()

        signIn(vm)
        advanceUntilIdle()

        assertNull(vm.uiState.value.navigateTo)
        assertNull(vm.uiState.value.error, "backing out is a choice, not a failure")
    }

    @Test
    fun reenrolledBiometricsDisarmTheSwitchAndSayWhy() = runTest(dispatcher) {
        armed()
        vault.nextResult = VaultResult.Invalidated
        val vm = viewModel()

        signIn(vm)
        advanceUntilIdle()

        assertFalse(authPreferences.isBiometricEnabled(), "it has to be set up again")
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.navigateTo)
    }

    @Test
    fun anExpiredRefreshTokenFallsBackToThePasswordForm() = runTest(dispatcher) {
        armed()
        val vm = viewModel(FaceIdAuthRepository(refreshWorks = false))

        signIn(vm)
        advanceUntilIdle()

        assertNull(vm.uiState.value.navigateTo)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }
}
