package com.hanmaum.dn.mobile.core.security

/**
 * Platform-backed encrypted key/value store for secrets that must never be
 * persisted in plaintext (e.g. saved login credentials for Face ID sign-in).
 *
 * - Android: AES/GCM key held in the AndroidKeyStore; ciphertext in app-private prefs.
 * - iOS: Keychain (KeychainSettings), accessible only after first device unlock.
 *
 * Distinct from [com.hanmaum.dn.mobile.core.domain.repository.TokenStorage], which
 * uses plain settings for non-secret session flags.
 */
interface SecureStore {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
}
