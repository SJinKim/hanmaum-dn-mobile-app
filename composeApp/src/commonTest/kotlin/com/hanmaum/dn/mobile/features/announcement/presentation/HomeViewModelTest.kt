package com.hanmaum.dn.mobile.features.announcement.presentation

import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.model.AnnouncementLookup
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeAnnouncementRepository : AnnouncementRepository {
    override suspend fun getAnnouncements(): List<Announcement> = emptyList()
    override suspend fun getAnnouncementById(id: String): AnnouncementLookup = AnnouncementLookup.NotFound
}

private class FakeNotificationRepository(
    private val unseen: Int = 0,
    private val failCount: Boolean = false,
) : NotificationRepository {
    val registeredTokens = mutableListOf<Pair<String, String>>()

    override suspend fun getNotifications(page: Int) =
        Result.success(NotificationPage(emptyList(), hasNext = false))
    override suspend fun getUnseenCount() =
        if (failCount) Result.failure(RuntimeException("boom")) else Result.success(unseen)
    override suspend fun markAllSeen(): Result<Unit> = Result.success(Unit)
    override suspend fun markRead(publicId: String): Result<Unit> = Result.success(Unit)
    override suspend fun markAllRead(): Result<Unit> = Result.success(Unit)
    override suspend fun getPushEnabled() = Result.success(true)
    override suspend fun setPushEnabled(enabled: Boolean) = Result.success(Unit)
    override suspend fun registerDeviceToken(token: String, platform: String): Result<Unit> {
        registeredTokens += token to platform
        return Result.success(Unit)
    }
    override suspend fun deleteDeviceToken(token: String): Result<Unit> = Result.success(Unit)
}

private class FakePushManager(private val token: String?) : PushManager {
    override val platform: String = "ANDROID"
    override suspend fun currentToken(): String? = token
    override fun isPermissionGranted(): Boolean = true
    override suspend fun requestPermission(): Boolean = true
}

class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `unseen count lands in ui state`() = runTest(dispatcher) {
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(unseen = 5),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(5, vm.uiState.value.unseenCount)
    }

    @Test
    fun `unseen count failure keeps zero`() = runTest(dispatcher) {
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(failCount = true),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.unseenCount)
    }

    @Test
    fun `registers device token on load when available`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakePushManager(token = "tok1"))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(listOf("tok1" to "ANDROID"), repo.registeredTokens)
    }

    @Test
    fun `null token skips registration`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakePushManager(token = null))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertTrue(repo.registeredTokens.isEmpty())
    }

    @Test
    fun `token registers only once per process`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakePushManager(token = "tok1"))
        vm.loadAnnouncements(); advanceUntilIdle()
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(1, repo.registeredTokens.size)
    }
}
