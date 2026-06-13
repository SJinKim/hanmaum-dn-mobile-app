package com.hanmaum.dn.mobile.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakePersistence(secured: Boolean = true) : RefreshTokenPersistence {
    var stored: String? = null
    private val deviceSecured = secured
    override fun isDeviceSecured() = deviceSecured
    override fun hasStored() = stored != null
    override fun store(token: String) { stored = token }
    override fun delete() { stored = null }
}

class RefreshTokenVaultTest {

    @Test
    fun `current is null before unlock even when a token is persisted`() {
        val store = FakePersistence().apply { stored = "persisted" }
        val vault = RefreshTokenVault(store)
        assertNull(vault.current())          // never read from disk without an unlock
        assertTrue(vault.hasStored())
    }

    @Test
    fun `store persists and caches in memory`() {
        val store = FakePersistence()
        val vault = RefreshTokenVault(store)
        vault.store("t1")
        assertEquals("t1", vault.current())
        assertEquals("t1", store.stored)
    }

    @Test
    fun `acceptUnlocked populates the in-memory session`() {
        val store = FakePersistence().apply { stored = "disk-token" }
        val vault = RefreshTokenVault(store)
        vault.acceptUnlocked("disk-token")
        assertEquals("disk-token", vault.current())
    }

    @Test
    fun `lock clears memory but keeps persisted token`() {
        val store = FakePersistence()
        val vault = RefreshTokenVault(store)
        vault.store("t1")
        vault.lock()
        assertNull(vault.current())
        assertTrue(vault.hasStored())
    }

    @Test
    fun `clear wipes memory and persistence`() {
        val store = FakePersistence()
        val vault = RefreshTokenVault(store)
        vault.store("t1")
        vault.clear()
        assertNull(vault.current())
        assertFalse(vault.hasStored())
    }
}
