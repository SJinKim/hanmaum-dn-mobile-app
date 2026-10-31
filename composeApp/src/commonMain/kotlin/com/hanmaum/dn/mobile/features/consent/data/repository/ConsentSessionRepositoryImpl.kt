package com.hanmaum.dn.mobile.features.consent.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.invalidateBearerCache
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.core.security.currentSessionScope
import com.hanmaum.dn.mobile.features.consent.domain.repository.ConsentSessionRepository
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class ConsentSessionRepositoryImpl(
    private val tokens: TokenStorage,
    private val client: HttpClient,
    private val notifications: NotificationRepository,
    private val push: PushManager,
    private val vault: BiometricVault,
    private val preferences: AuthPreferences,
) : ConsentSessionRepository {
    override fun scope(): String? = tokens.currentSessionScope()

    override suspend fun logout(forgetBiometric: Boolean) {
        // Cleanup is bounded and best-effort; a network outage cannot trap the member.
        try {
            withTimeoutOrNull(3_000) {
                push.currentToken()?.let { notifications.deleteDeviceToken(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Push cleanup is best-effort; local logout must still complete.
        } finally {
            tokens.clear()
            client.invalidateBearerCache()
            if (forgetBiometric) {
                preferences.setBiometricEnabled(false)
                vault.clear()
            }
        }
    }
}
