package com.hanmaum.dn.mobile.core.security

/**
 * Bridges the gated [RefreshTokenPersistence] and the running session. The
 * offline token only enters [current] after a successful unlock (or a fresh
 * store at login); silent token refresh reads [current] synchronously and never
 * prompts. On background the session calls [lock] to drop the in-memory copy.
 */
class RefreshTokenVault(private val persistence: RefreshTokenPersistence) {

    private var inMemory: String? = null

    /** In-memory token for this foreground session; null when locked. */
    fun current(): String? = inMemory

    fun hasStored(): Boolean = persistence.hasStored()

    fun isDeviceSecured(): Boolean = persistence.isDeviceSecured()

    /** Persist (free, no prompt) after login or token rotation, and cache it. */
    fun store(token: String) {
        persistence.store(token)
        inMemory = token
    }

    /** Cache a token just read via a biometric unlock. */
    fun acceptUnlocked(token: String) { inMemory = token }

    /** Drop the in-memory copy (on background); persisted token survives. */
    fun lock() { inMemory = null }

    /** Wipe both memory and persistent storage (logout). */
    fun clear() {
        inMemory = null
        persistence.delete()
    }
}
