package com.hanmaum.dn.mobile.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class InMemorySecureStore : SecureStore {
    private val map = mutableMapOf<String, String>()
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String): String? = map[key]
    override fun remove(key: String) { map.remove(key) }
}

class CredentialStoreTest {

    @Test
    fun roundTripsSavedCredentials() {
        val store = CredentialStore(InMemorySecureStore())
        store.saveCredentials("user@example.com", "Secret123!")

        assertTrue(store.hasCredentials())
        assertEquals(Credentials("user@example.com", "Secret123!"), store.getCredentials())
    }

    @Test
    fun hasCredentialsFalseWhenEmpty() {
        val store = CredentialStore(InMemorySecureStore())
        assertFalse(store.hasCredentials())
        assertNull(store.getCredentials())
    }

    @Test
    fun clearRemovesCredentials() {
        val store = CredentialStore(InMemorySecureStore())
        store.saveCredentials("user@example.com", "Secret123!")
        store.clear()

        assertFalse(store.hasCredentials())
        assertNull(store.getCredentials())
    }
}
