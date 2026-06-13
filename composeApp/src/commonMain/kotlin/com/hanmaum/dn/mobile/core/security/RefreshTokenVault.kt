package com.hanmaum.dn.mobile.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the gated [RefreshTokenPersistence] and the running session. The
 * offline token only enters [current] after a successful unlock (or a fresh
 * store at login); silent token refresh reads [current] synchronously and never
 * prompts. On background the session calls [lock] to drop the in-memory copy.
 *
 * [unlocked] lets startup logic (the splash session check) wait until the token
 * is actually available before making any authed call — otherwise an expired
 * access token would trigger a refresh with no in-memory refresh token and the
 * resulting 401 would destroy the session.
 */
class RefreshTokenVault(private val persistence: RefreshTokenPersistence) {

    private var inMemory: String? = null

    private val _unlocked = MutableStateFlow(false)
    /** True once the token is in memory for this foreground session. */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /** In-memory token for this foreground session; null when locked. */
    fun current(): String? = inMemory

    fun hasStored(): Boolean = persistence.hasStored()

    fun isDeviceSecured(): Boolean = persistence.isDeviceSecured()

    /** Persist (free, no prompt) after login or token rotation, and cache it. */
    fun store(token: String) {
        persistence.store(token)
        inMemory = token
        _unlocked.value = true
    }

    /** Cache a token just read via a biometric unlock. */
    fun acceptUnlocked(token: String) {
        inMemory = token
        _unlocked.value = true
    }

    /** Drop the in-memory copy (on background); persisted token survives. */
    fun lock() {
        inMemory = null
        _unlocked.value = false
    }

    /** Wipe both memory and persistent storage (logout). */
    fun clear() {
        inMemory = null
        _unlocked.value = false
        persistence.delete()
    }
}
