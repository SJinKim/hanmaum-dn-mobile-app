package com.hanmaum.dn.mobile.features.login.presentation

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.security.FakeSecureStore
import com.hanmaum.dn.mobile.core.security.FakeBiometricVault
import com.hanmaum.dn.mobile.core.security.VaultResult
import com.hanmaum.dn.mobile.core.security.BrowserAuthentication
import com.hanmaum.dn.mobile.features.login.domain.model.LoginException
import io.ktor.http.Url
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.TestScope
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
    var exchanges = 0
    override suspend fun exchangeAuthorizationCode(code: String, verifier: String): TokenResponse {
        exchanges++
        return TokenResponse(
        accessToken = "access", expiresIn = 300, refreshToken = "refresh", tokenType = "Bearer",
        )
    }
    override suspend fun refresh(refreshToken: String): TokenResponse {
        refreshedWith = refreshToken
        if (!refreshWorks) throw LoginException(400, "invalid_grant", "expired")
        return TokenResponse(
            accessToken = "fresh-access", expiresIn = 300,
            refreshToken = "rotated-refresh", tokenType = "Bearer",
        )
    }
    override suspend fun register(request: RegisterRequest): Result<Unit> = Result.success(Unit)
}

private class FaceIdMemberRepository(private val status: MemberStatus = MemberStatus.ACTIVE,
    private val fails: Boolean = false) : MemberRepository {
    override suspend fun getMyProfile(): Result<MemberResponse> = if (fails) Result.failure(IllegalStateException("offline")) else Result.success(
        MemberResponse(publicId = "p1", firstName = "서진", lastName = "김", status = status),
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

    private fun browser(cancel: Boolean = false, invalidState: Boolean = false) = object : BrowserAuthentication {
        override suspend fun authenticate(url: String, callbackScheme: String): String? {
            if (cancel) return null
            val params = Url(url).parameters
            val state = if (invalidState) "wrong" else params["state"]
            return "${params["redirect_uri"]}?code=code&state=$state"
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val settings = MapSettings()
    private val authPreferences = AuthPreferencesImpl(settings)
    private val tokenStorage = TokenStorageImpl(FakeSecureStore(), settings)
    private val vault = FakeBiometricVault()

    private fun viewModel(auth: AuthRepository = FaceIdAuthRepository(), members: MemberRepository = FaceIdMemberRepository()) = LoginViewModel(
        authRepository = auth,
        memberRepository = members,
        tokenStorage = tokenStorage,
        httpClient = HttpClient(MockEngine { respond("") }),
        authPreferences = authPreferences,
        biometricVault = vault,
    )

    private suspend fun signIn(vm: LoginViewModel) =
        vm.signInWithFaceId(vault, "title", "subtitle", "cancel")

    /** What the 설정 switch leaves behind once Face ID is on. */
    private suspend fun armed() {
        authPreferences.setBiometricEnabled(true)
        vault.seal("sealed-refresh", "t", "s", "c")
    }

    /** First refresh suspends until the composition-bound sign-in job is cancelled. */
    private fun interruptedRefreshAuth(): AuthRepository {
        val delegate = FaceIdAuthRepository()
        return object : AuthRepository by delegate {
            private var firstRefresh = true
            override suspend fun refresh(refreshToken: String): TokenResponse {
                if (firstRefresh) {
                    firstRefresh = false
                    awaitCancellation()
                }
                return delegate.refresh(refreshToken)
            }
        }
    }

    private suspend fun TestScope.cancelInFlightFaceIdRefresh(vm: LoginViewModel) {
        val job = launch { signIn(vm) }
        runCurrent()
        assertTrue(vm.uiState.value.isLoading, "refresh really is in flight")
        job.cancelAndJoin()
        assertTrue(job.isCancelled, "cancellation must still propagate")
        assertFalse(vm.uiState.value.isLoading, "login must be usable again")
        assertEquals("", vm.uiState.value.statusMessage)
        assertNull(vm.uiState.value.error, "cancellation is silent")
        assertNull(vm.uiState.value.navigateTo)
        assertNull(tokenStorage.getAccessToken())
        assertTrue(vm.canFaceIdSignIn(vault), "cancellation must not disarm Face ID")
        assertEquals("sealed-refresh", vault.sealed)
    }

    @Test
    fun cancelledFaceIdRefreshAllowsBrowserSignInOnSameViewModel() = runTest(dispatcher) {
        armed()
        val vm = viewModel(interruptedRefreshAuth())
        cancelInFlightFaceIdRefresh(vm)
        vm.onLoginClicked(browser())
        advanceUntilIdle()
        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun cancelledFaceIdRefreshAllowsExplicitFaceIdRetryOnSameViewModel() = runTest(dispatcher) {
        armed()
        val vm = viewModel(interruptedRefreshAuth())
        cancelInFlightFaceIdRefresh(vm)
        signIn(vm)
        advanceUntilIdle()
        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
        assertEquals("rotated-refresh", vault.sealed)
        assertNull(vm.uiState.value.error)
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
    fun anotherMemberSigningInWithBrowserClearsTheVault() = runTest(dispatcher) {
        // The arming outlives a sign-out, so the button must not survive into
        // somebody else's session.
        armed()
        authPreferences.setBiometricMemberId("someone-else")
        val vm = viewModel()

        vm.onLoginClicked(browser())
        advanceUntilIdle()

        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(vault.sealed)
    }

    @Test
    fun theSameMemberSigningInWithBrowserKeepsTheVault() = runTest(dispatcher) {
        armed()
        authPreferences.setBiometricMemberId("p1")
        val vm = viewModel()

        vm.onLoginClicked(browser())
        advanceUntilIdle()

        assertTrue(authPreferences.isBiometricEnabled(), "same member, nothing to protect against")
        assertEquals("sealed-refresh", vault.sealed)
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
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.canFaceIdSignIn(vault))
        vm.onLoginClicked(browser())
        advanceUntilIdle()
        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
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
    fun anExpiredRefreshTokenFallsBackToBrowserAndClearsDeadVault() = runTest(dispatcher) {
        armed()
        val vm = viewModel(FaceIdAuthRepository(refreshWorks = false))

        signIn(vm)
        advanceUntilIdle()

        assertNull(vm.uiState.value.navigateTo)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(authPreferences.isBiometricEnabled())
        assertNull(vault.sealed)
        assertTrue(vm.uiState.value.biometricExpired)
    }

    @Test fun browserReceivesSelectedResetLanguageAndCancellationLeavesLoginUsable() = runTest(dispatcher) {
        val auth = FaceIdAuthRepository()
        val vm = viewModel(auth)
        val capture = object : BrowserAuthentication {
            override suspend fun authenticate(url: String, callbackScheme: String): String? {
                assertEquals("de", Url(url).parameters["ui_locales"])
                return null
            }
        }
        vm.onLoginClicked(capture, uiLocale = "de")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(0, auth.exchanges)
        vm.onLoginClicked(browser(), uiLocale = "de")
        advanceUntilIdle()
        assertEquals(1, auth.exchanges)
    }

    @Test fun browserCancellationIsSilent() = runTest(dispatcher) {
        val auth = FaceIdAuthRepository()
        val vm = viewModel(auth)
        vm.onLoginClicked(browser(cancel = true))
        advanceUntilIdle()
        assertEquals(0, auth.exchanges)
        assertNull(vm.uiState.value.error)
        assertNull(tokenStorage.getAccessToken())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun invalidBrowserStateNeverReachesTokenExchange() = runTest(dispatcher) {
        val auth = FaceIdAuthRepository()
        val vm = viewModel(auth)
        vm.onLoginClicked(browser(invalidState = true))
        advanceUntilIdle()
        assertEquals(0, auth.exchanges)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun doubleTapStartsOnlyOneAuthorization() = runTest(dispatcher) {
        val auth = FaceIdAuthRepository()
        val vm = viewModel(auth)
        vm.onLoginClicked(browser())
        vm.onLoginClicked(browser())
        advanceUntilIdle()
        assertEquals(1, auth.exchanges)
        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
        assertTrue(authPreferences.isKeepSignedInEnabled())
    }

    @Test fun browserRoutesPendingAndRejectedFromRealProfileStatus() = runTest(dispatcher) {
        listOf(MemberStatus.PENDING to NavRoute.PendingApproval, MemberStatus.REJECTED to NavRoute.Rejected).forEach { (status, route) ->
            val vm = viewModel(members = FaceIdMemberRepository(status))
            vm.onLoginClicked(browser())
            advanceUntilIdle()
            assertEquals(route, vm.uiState.value.navigateTo)
        }
    }

    @Test fun failedProfileClearsNewSessionAndOffersRetry() = runTest(dispatcher) {
        val vm = viewModel(members = FaceIdMemberRepository(fails = true))
        vm.onLoginClicked(browser())
        advanceUntilIdle()
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(vm.uiState.value.navigateTo)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun cancellationCanBeFollowedBySuccessfulRetry() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onLoginClicked(browser(cancel = true))
        advanceUntilIdle()
        vm.onLoginClicked(browser())
        advanceUntilIdle()
        assertEquals(NavRoute.Home, vm.uiState.value.navigateTo)
        assertNull(vm.uiState.value.error)
    }
}
