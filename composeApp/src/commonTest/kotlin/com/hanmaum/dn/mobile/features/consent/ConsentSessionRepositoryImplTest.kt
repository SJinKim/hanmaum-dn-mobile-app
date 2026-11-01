package com.hanmaum.dn.mobile.features.consent

import com.hanmaum.dn.mobile.core.data.repository.AuthPreferencesImpl
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.core.security.FakeBiometricVault
import com.hanmaum.dn.mobile.features.consent.data.repository.ConsentSessionRepositoryImpl
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

private class FakeTokenStorage : TokenStorage {
    private var access: String? = null
    private var refresh: String? = null
    override fun saveAccessToken(token: String) { access = token }
    override fun getAccessToken(): String? = access
    override fun saveRefreshToken(token: String?) { refresh = token }
    override fun getRefreshToken(): String? = refresh
    override fun clear() { access = null; refresh = null }
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
    override val platform: String = "ANDROID"
    override suspend fun currentToken(): String? = token
    override fun isPermissionGranted(): Boolean = true
    override suspend fun requestPermission(): Boolean = true
}

class ConsentSessionRepositoryImplTest {
    private val tokens = FakeTokenStorage()
    private val vault = FakeBiometricVault()
    private val preferences = AuthPreferencesImpl(MapSettings())

    private fun repository(
        notifications: FakeNotificationRepository = FakeNotificationRepository(),
        push: FakePushManager = FakePushManager(),
    ) = ConsentSessionRepositoryImpl(
        tokens, HttpClient(MockEngine { respondOk() }), notifications, push, vault, preferences,
    )

    @Test
    fun `logout deletes the device token before clearing the session`() = runTest {
        tokens.saveAccessToken("access")
        val notifications = FakeNotificationRepository()
        repository(notifications, FakePushManager(token = "tok1")).logout()
        assertEquals(listOf("tok1"), notifications.deletedTokens)
        assertNull(tokens.getAccessToken())
    }

    @Test
    fun `logout without token still clears the session`() = runTest {
        tokens.saveAccessToken("access")
        val notifications = FakeNotificationRepository()
        repository(notifications).logout()
        assertEquals(emptyList(), notifications.deletedTokens)
        assertNull(tokens.getAccessToken())
    }

    @Test
    fun `logout leaves the face id arming alone by default`() = runTest {
        // Signing out is how a session ordinarily ends. Disarming here left the
        // member on a login screen that never offered Face ID, so there was no
        // way to reach it at all (#218). Only their face opens what stays behind.
        vault.seal("sealed-refresh", "t", "s", "c")
        preferences.setBiometricEnabled(true)

        repository().logout()

        assertEquals(true, preferences.isBiometricEnabled())
        assertEquals("sealed-refresh", vault.sealed)
        assertEquals(false, vault.cleared)
    }

    @Test
    fun `logout can forget the face id arming`() = runTest {
        vault.seal("sealed-refresh", "t", "s", "c")
        preferences.setBiometricEnabled(true)

        repository().logout(forgetBiometric = true)

        assertEquals(false, preferences.isBiometricEnabled())
        assertEquals(true, vault.cleared)
    }
}
