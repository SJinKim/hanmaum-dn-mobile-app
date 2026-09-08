package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenStorageImplTest {

    private fun storage(settings: MapSettings = MapSettings()) = TokenStorageImpl(settings)

    @Test
    fun `access and refresh tokens round-trip`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.saveAccessToken("access-123")
        s.saveRefreshToken("refresh-456")
        assertEquals("access-123", storage(settings).getAccessToken())
        assertEquals("refresh-456", storage(settings).getRefreshToken())
    }

    @Test
    fun `clear drops both tokens`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.saveAccessToken("a")
        s.saveRefreshToken("r")

        s.clear()

        assertNull(storage(settings).getAccessToken())
        assertNull(storage(settings).getRefreshToken())
    }

    @Test
    fun `clear leaves the member's login preferences alone`() {
        // A session-only teardown must not switch Face ID off — that is exactly
        // when the login screen needs it to offer the saved-credential prompt.
        val settings = MapSettings()
        val prefs = AuthPreferencesImpl(settings)
        prefs.setBiometricEnabled(true)

        storage(settings).clear()

        assertTrue(AuthPreferencesImpl(settings).isBiometricEnabled())
        assertTrue(AuthPreferencesImpl(settings).isKeepSignedInEnabled())
    }
}
