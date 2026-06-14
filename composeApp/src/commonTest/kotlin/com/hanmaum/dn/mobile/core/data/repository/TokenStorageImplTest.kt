package com.hanmaum.dn.mobile.core.data.repository

import com.hanmaum.dn.mobile.core.security.RefreshTokenPersistence
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class NoopPersistence : RefreshTokenPersistence {
    var stored: String? = null
    override fun isDeviceSecured() = true
    override fun hasStored() = stored != null
    override fun store(token: String) { stored = token }
    override fun delete() { stored = null }
}

class TokenStorageImplTest {

    private fun storage(settings: MapSettings = MapSettings()) =
        TokenStorageImpl(settings, RefreshTokenVault(NoopPersistence()))

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
    fun `clear removes access token and resets keepSignedIn to default true`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.saveAccessToken("a")
        s.setKeepSignedIn(false)

        s.clear()

        assertNull(storage(settings).getAccessToken())
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
    fun `access token round-trips`() {
        val settings = MapSettings()
        val s = storage(settings)
        s.saveAccessToken("access-123")
        // getAccessToken reads from Settings — uses the same shared MapSettings instance
        assertTrue(storage(settings).getAccessToken() == "access-123")
    }
}
