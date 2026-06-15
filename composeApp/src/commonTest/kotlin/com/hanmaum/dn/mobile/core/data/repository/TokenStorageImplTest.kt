package com.hanmaum.dn.mobile.core.data.repository

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenStorageImplTest {

    private fun storage(settings: MapSettings = MapSettings()) = TokenStorageImpl(settings)

    @Test
    fun `isKeepSignedIn defaults to true when nothing stored`() {
        assertTrue(storage().isKeepSignedIn())
    }

    @Test
    fun `setKeepSignedIn false round-trips`() {
        val settings = MapSettings()
        storage(settings).setKeepSignedIn(false)
        assertFalse(storage(settings).isKeepSignedIn())
    }

    @Test
    fun `setKeepSignedIn true round-trips`() {
        val settings = MapSettings()
        storage(settings).setKeepSignedIn(true)
        assertTrue(storage(settings).isKeepSignedIn())
    }

    @Test
    fun `clear removes tokens and resets keepSignedIn to default true`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.saveAccessToken("a")
        s.saveRefreshToken("r")
        s.setKeepSignedIn(false)

        s.clear()

        assertNull(storage(settings).getAccessToken())
        assertNull(storage(settings).getRefreshToken())
        assertTrue(storage(settings).isKeepSignedIn())
    }

    @Test
    fun `isBiometricEnabled defaults to false`() {
        assertFalse(storage().isBiometricEnabled())
    }

    @Test
    fun `setBiometricEnabled true round-trips`() {
        val settings = MapSettings()
        storage(settings).setBiometricEnabled(true)
        assertTrue(storage(settings).isBiometricEnabled())
    }

    @Test
    fun `clear resets biometricEnabled to false`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.setBiometricEnabled(true)
        s.clear()
        assertFalse(storage(settings).isBiometricEnabled())
    }

    @Test
    fun `access and refresh tokens round-trip`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.saveAccessToken("access-123")
        s.saveRefreshToken("refresh-456")
        assertEquals("access-123", storage(settings).getAccessToken())
        assertEquals("refresh-456", storage(settings).getRefreshToken())
    }
}
