package com.hanmaum.dn.mobile.features.announcement.presentation

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
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

private class FakeAnnouncementRepository : AnnouncementRepository {
    override suspend fun getAnnouncements(): List<Announcement> = emptyList()
    override suspend fun getAnnouncementById(id: String): Announcement? = null
}

private class FakeNotificationRepository(
    private val unseen: Int = 0,
    private val failCount: Boolean = false,
) : NotificationRepository {
    override suspend fun getNotifications(page: Int) =
        Result.success(NotificationPage(emptyList(), hasNext = false))
    override suspend fun getUnseenCount() =
        if (failCount) Result.failure(RuntimeException("boom")) else Result.success(unseen)
    override suspend fun markAllSeen(): Result<Unit> = Result.success(Unit)
    override suspend fun markRead(publicId: String): Result<Unit> = Result.success(Unit)
    override suspend fun markAllRead(): Result<Unit> = Result.success(Unit)
    override suspend fun getPushEnabled() = Result.success(true)
    override suspend fun setPushEnabled(enabled: Boolean) = Result.success(Unit)
    override suspend fun registerDeviceToken(token: String, platform: String) = Result.success(Unit)
    override suspend fun deleteDeviceToken(token: String) = Result.success(Unit)
}

class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `unseen count lands in ui state`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeAnnouncementRepository(), FakeNotificationRepository(unseen = 5))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(5, vm.uiState.value.unseenCount)
    }

    @Test
    fun `unseen count failure keeps zero`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeAnnouncementRepository(), FakeNotificationRepository(failCount = true))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.unseenCount)
    }
}
