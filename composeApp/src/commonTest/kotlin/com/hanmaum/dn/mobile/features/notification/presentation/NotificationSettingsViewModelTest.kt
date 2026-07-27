package com.hanmaum.dn.mobile.features.notification.presentation

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

class NotificationSettingsViewModelTest {
    // Nested (not top-level) because NotificationListViewModelTest.kt already declares a
    // top-level `private class FakeNotificationRepository` in this same package — Kotlin
    // private top-level classes still bind to a single package-level JVM class name, so a
    // second one collides at compile time. Nesting it here keeps this file's fake local to
    // this file per the brief while avoiding that clash.
    private class FakeNotificationRepository : NotificationRepository {
        var pushEnabled = true
        val setPushEnabledCalls = mutableListOf<Boolean>()
        var failSetPush = false

        override suspend fun getNotifications(page: Int) =
            Result.success(NotificationPage(emptyList(), hasNext = false))
        override suspend fun getUnseenCount() = Result.success(0)
        override suspend fun markAllSeen(): Result<Unit> = Result.success(Unit)
        override suspend fun markRead(publicId: String): Result<Unit> = Result.success(Unit)
        override suspend fun markAllRead(): Result<Unit> = Result.success(Unit)
        override suspend fun delete(publicId: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAll(): Result<Unit> = Result.success(Unit)
        override suspend fun getPushEnabled() = Result.success(pushEnabled)
        override suspend fun setPushEnabled(enabled: Boolean): Result<Unit> {
            setPushEnabledCalls += enabled
            return if (failSetPush) Result.failure(RuntimeException("boom")) else Result.success(Unit)
        }
        override suspend fun registerDeviceToken(token: String, platform: String) = Result.success(Unit)
        override suspend fun deleteDeviceToken(token: String) = Result.success(Unit)
    }

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load reads server flag`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        repo.pushEnabled = false
        val vm = NotificationSettingsViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertEquals(false, vm.uiState.value.pushEnabled)
    }

    @Test
    fun `toggle optimistically updates and calls server`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationSettingsViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.onToggle(false); advanceUntilIdle()
        assertEquals(false, vm.uiState.value.pushEnabled)
        assertEquals(listOf(false), repo.setPushEnabledCalls)
    }

    @Test
    fun `toggle reverts on server failure`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository().apply { failSetPush = true }
        val vm = NotificationSettingsViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.onToggle(false); advanceUntilIdle()
        assertEquals(true, vm.uiState.value.pushEnabled)
    }
}
