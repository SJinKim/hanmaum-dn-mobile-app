package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.security.FakeSecureStore
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenStorageImplTest {

    private fun storage(
        secure: FakeSecureStore = FakeSecureStore(),
        settings: MapSettings = MapSettings(),
    ) = TokenStorageImpl(secure, settings)

    @Test
    fun `access and refresh tokens round-trip`() {
        val secure = FakeSecureStore()
        val settings = MapSettings()
        val s = storage(secure, settings)
        s.saveAccessToken("access-123")
        s.saveRefreshToken("refresh-456")
        assertEquals("access-123", storage(secure, settings).getAccessToken())
        assertEquals("refresh-456", storage(secure, settings).getRefreshToken())
    }

    @Test
    fun `clear drops both tokens`() {
        val secure = FakeSecureStore()
        val settings = MapSettings()
        val s = storage(secure, settings)
        s.saveAccessToken("a")
        s.saveRefreshToken("r")

        s.clear()

        assertNull(storage(secure, settings).getAccessToken())
        assertNull(storage(secure, settings).getRefreshToken())
    }

    @Test
    fun `clear leaves the member's login preferences alone`() {
        // A session-only teardown must not switch Face ID off — that is exactly
        // when the login screen needs it to offer the prompt.
        val settings = MapSettings()
        val prefs = AuthPreferencesImpl(settings)
        prefs.setBiometricEnabled(true)

        storage(FakeSecureStore(), settings).clear()

        assertTrue(AuthPreferencesImpl(settings).isBiometricEnabled())
        assertTrue(AuthPreferencesImpl(settings).isKeepSignedInEnabled())
    }

    @Test
    fun `nothing readable is left in plain settings`() {
        // The whole point: a refresh token in NSUserDefaults made the Face ID
        // vault pointless, because the same token lay unprotected beside it.
        val settings = MapSettings()
        val s = storage(FakeSecureStore(), settings)

        s.saveAccessToken("access-123")
        s.saveRefreshToken("refresh-456")

        assertNull(settings.getStringOrNull("access_token"))
        assertNull(settings.getStringOrNull("refresh_token"))
    }

    @Test
    fun `an update carries the signed-in member across`() {
        // What the old build left behind. Dropping it would sign everyone out.
        val settings = MapSettings()
        settings.putString("access_token", "old-access")
        settings.putString("refresh_token", "old-refresh")
        val secure = FakeSecureStore()

        val s = storage(secure, settings)

        assertEquals("old-access", s.getAccessToken())
        assertEquals("old-refresh", s.getRefreshToken())
        assertNull(settings.getStringOrNull("refresh_token"), "and the plain copy is gone")
    }
}
