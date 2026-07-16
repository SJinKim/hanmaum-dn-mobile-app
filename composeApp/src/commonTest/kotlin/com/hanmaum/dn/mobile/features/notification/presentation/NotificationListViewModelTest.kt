package com.hanmaum.dn.mobile.features.notification.presentation

import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationReference
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun sample(id: String, read: Boolean = false) = Notification(
    publicId = id,
    type = NotificationType.ANNOUNCEMENT,
    title = "새로운 소식이 있습니다!",
    body = "본문",
    reference = NotificationReference(NotificationType.ANNOUNCEMENT, "a-$id"),
    createdAt = Instant.parse("2026-07-15T10:00:00Z"),
    isSeen = false,
    isRead = read,
)

private class FakeNotificationRepository : NotificationRepository {
    var pages = mutableMapOf(0 to NotificationPage(listOf(sample("n1"), sample("n2", read = true)), hasNext = true),
                             1 to NotificationPage(listOf(sample("n3")), hasNext = false))
    var markAllSeenCalls = 0
    var markReadIds = mutableListOf<String>()
    var markAllReadCalls = 0
    var failList = false

    override suspend fun getNotifications(page: Int) =
        if (failList) Result.failure(RuntimeException("boom")) else Result.success(pages.getValue(page))
    override suspend fun getUnseenCount() = Result.success(0)
    override suspend fun markAllSeen(): Result<Unit> { markAllSeenCalls++; return Result.success(Unit) }
    override suspend fun markRead(publicId: String): Result<Unit> { markReadIds += publicId; return Result.success(Unit) }
    override suspend fun markAllRead(): Result<Unit> { markAllReadCalls++; return Result.success(Unit) }
    override suspend fun getPushEnabled() = Result.success(true)
    override suspend fun setPushEnabled(enabled: Boolean) = Result.success(Unit)
    override suspend fun registerDeviceToken(token: String, platform: String) = Result.success(Unit)
    override suspend fun deleteDeviceToken(token: String) = Result.success(Unit)
}

class NotificationListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load populates items and marks all seen`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertEquals(2, vm.uiState.value.items.size)
        assertTrue(vm.uiState.value.hasNext)
        assertEquals(1, repo.markAllSeenCalls)
    }

    @Test
    fun `item click marks read optimistically and emits navigation`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        var navigated: String? = null
        val job = launch { navigated = vm.openAnnouncement.first() }
        advanceUntilIdle() // Let launch block start listening
        vm.onItemClick(vm.uiState.value.items[0]); advanceUntilIdle()
        assertTrue(vm.uiState.value.items[0].isRead)
        assertEquals(listOf("n1"), repo.markReadIds)
        assertEquals("a-n1", navigated)
        job.cancel()
    }

    @Test
    fun `read all flips every item`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.onReadAll(); advanceUntilIdle()
        assertTrue(vm.uiState.value.allRead)
        assertEquals(1, repo.markAllReadCalls)
    }

    @Test
    fun `load more appends next page`() = runTest(dispatcher) {
        val vm = NotificationListViewModel(FakeNotificationRepository())
        vm.load(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertEquals(3, vm.uiState.value.items.size)
        assertEquals(false, vm.uiState.value.hasNext)
    }

    @Test
    fun `load failure surfaces error`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository().apply { failList = true }
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value.error != null)
    }
}
