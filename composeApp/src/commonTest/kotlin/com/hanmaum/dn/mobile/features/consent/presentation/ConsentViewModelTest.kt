package com.hanmaum.dn.mobile.features.consent.presentation

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.features.consent.FakeConsentRepository
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentResult
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentState
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentViewModelTest {

    // Nested, so they cannot clash with same-named fakes elsewhere in the test source set.
    private class FakeTokenStorage : TokenStorage {
        var cleared = false
        override fun saveAccessToken(token: String) = Unit
        override fun getAccessToken(): String? = null
        override fun saveRefreshToken(token: String?) = Unit
        override fun getRefreshToken(): String? = null
        override fun clear() { cleared = true }
    }

    private class FakeNotificationRepository : NotificationRepository {
        val deletedTokens = mutableListOf<String>()

        override suspend fun getNotifications(page: Int) =
            Result.success(NotificationPage(emptyList(), hasNext = false))
        override suspend fun getUnseenCount() = Result.success(0)
        override suspend fun markAllSeen(): Result<Unit> = Result.success(Unit)
        override suspend fun markRead(publicId: String): Result<Unit> = Result.success(Unit)
        override suspend fun markAllRead(): Result<Unit> = Result.success(Unit)
        override suspend fun delete(publicId: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAll(): Result<Unit> = Result.success(Unit)
        override suspend fun getPushEnabled() = Result.success(true)
        override suspend fun setPushEnabled(enabled: Boolean) = Result.success(Unit)
        override suspend fun registerDeviceToken(token: String, platform: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteDeviceToken(token: String): Result<Unit> {
            deletedTokens += token
            return Result.success(Unit)
        }
    }

    private class FakePushManager(private val token: String? = null) : PushManager {
        override val platform: String = "IOS"
        override suspend fun currentToken(): String? = token
        override fun isPermissionGranted(): Boolean = true
        override suspend fun requestPermission(): Boolean = true
    }

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeConsentRepository()
    private val tokenStorage = FakeTokenStorage()
    private val notifications = FakeNotificationRepository()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun vm(pushToken: String? = null) = ConsentViewModel(
        repo,
        tokenStorage,
        HttpClient(MockEngine { respond("") }),
        notifications,
        FakePushManager(pushToken),
    )

    @Test
    fun `load exposes the APP_PRIVACY status`() = runTest(dispatcher) {
        repo.myConsentsResult = ConsentResult.Success(
            FakeConsentRepository.overview(ConsentState.OUTDATED, grantedVersion = "2026-01"),
        )
        val viewModel = vm()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
        assertEquals(ConsentState.OUTDATED, state.status?.state)
        assertEquals("2026-01", state.status?.grantedVersion)
        assertFalse(state.isGranted)
    }

    @Test
    fun `a failed load shows the error and a reload fetches again`() = runTest(dispatcher) {
        repo.myConsentsResult = ConsentResult.Failed
        val viewModel = vm()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.loadFailed)
        assertNull(viewModel.uiState.value.status)

        repo.myConsentsResult = ConsentResult.Success(FakeConsentRepository.overview(ConsentState.MISSING))
        viewModel.load()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(ConsentState.MISSING, viewModel.uiState.value.status?.state)
        assertEquals(2, repo.myConsentsCalls)
    }

    @Test
    fun `grant needs the box ticked`() = runTest(dispatcher) {
        repo.myConsentsResult = ConsentResult.Success(FakeConsentRepository.overview(ConsentState.MISSING))
        val viewModel = vm()
        advanceUntilIdle()

        viewModel.grant()
        advanceUntilIdle()

        assertEquals(0, repo.grantCalls)
        assertNull(viewModel.uiState.value.navigateTo)
    }

    @Test
    fun `grant sends the version shown and continues`() = runTest(dispatcher) {
        repo.myConsentsResult = ConsentResult.Success(FakeConsentRepository.overview(ConsentState.MISSING))
        val viewModel = vm()
        advanceUntilIdle()

        viewModel.onAcceptedChange(true)
        viewModel.grant()
        advanceUntilIdle()

        assertEquals(ConsentPurpose.APP_PRIVACY to "2026-09", repo.lastGrant)
        assertEquals(ConsentNavigation.Continue, viewModel.uiState.value.navigateTo)

        viewModel.onNavigationHandled()
        assertNull(viewModel.uiState.value.navigateTo)
    }

    @Test
    fun `an outdated version unticks the box and reloads`() = runTest(dispatcher) {
        repo.myConsentsResult = ConsentResult.Success(FakeConsentRepository.overview(ConsentState.MISSING))
        repo.grantResult = ConsentResult.VersionOutdated
        val viewModel = vm()
        advanceUntilIdle()

        viewModel.onAcceptedChange(true)
        viewModel.grant()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.accepted)
        assertFalse(state.isSaving)
        assertFalse(state.saveFailed)
        assertNull(state.navigateTo)
        assertEquals(2, repo.myConsentsCalls)
    }

    @Test
    fun `a failed grant keeps the box ticked and shows the error`() = runTest(dispatcher) {
        repo.myConsentsResult = ConsentResult.Success(FakeConsentRepository.overview(ConsentState.MISSING))
        repo.grantResult = ConsentResult.Failed
        val viewModel = vm()
        advanceUntilIdle()

        viewModel.onAcceptedChange(true)
        viewModel.grant()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.accepted)
        assertTrue(state.saveFailed)
        assertFalse(state.isSaving)
        assertNull(state.navigateTo)
    }

    @Test
    fun `withdraw marks the consent missing and navigates`() = runTest(dispatcher) {
        val viewModel = vm()
        advanceUntilIdle()

        viewModel.onWithdrawRequested()
        assertTrue(viewModel.uiState.value.showWithdrawDialog)
        viewModel.withdraw()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ConsentPurpose.APP_PRIVACY, repo.lastWithdrawn)
        assertEquals(ConsentState.MISSING, state.status?.state)
        assertNull(state.status?.grantedVersion)
        assertFalse(state.showWithdrawDialog)
        assertEquals(ConsentNavigation.Withdrawn, state.navigateTo)
    }

    @Test
    fun `a failed withdraw keeps the dialog open with the error`() = runTest(dispatcher) {
        repo.withdrawResult = ConsentResult.Failed
        val viewModel = vm()
        advanceUntilIdle()

        viewModel.onWithdrawRequested()
        viewModel.withdraw()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showWithdrawDialog)
        assertTrue(state.withdrawFailed)
        assertTrue(state.isGranted)
        assertNull(state.navigateTo)
    }

    @Test
    fun `logout drops the push token and clears the session`() = runTest(dispatcher) {
        val viewModel = vm(pushToken = "device-1")
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(listOf("device-1"), notifications.deletedTokens)
        assertTrue(tokenStorage.cleared)
        assertEquals(ConsentNavigation.LoggedOut, viewModel.uiState.value.navigateTo)
    }
}
