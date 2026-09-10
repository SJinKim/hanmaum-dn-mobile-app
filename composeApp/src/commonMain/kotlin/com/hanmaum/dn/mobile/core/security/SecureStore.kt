package com.hanmaum.dn.mobile.core.security

/**
 * Platform-backed encrypted key/value store for secrets that must never be
 * persisted in plaintext.
 *
 * - Android: AES/GCM key held in the AndroidKeyStore; ciphertext in app-private prefs.
 * - iOS: Keychain (KeychainSettings), accessible only after first device unlock.
 *
 * This is where the session tokens live. The line that used to stand here said
 * `TokenStorage` kept "non-secret session flags" in plain settings — it kept the
 * access and refresh tokens, which is the opposite of non-secret, and it is what
 * made the Face ID vault guard a door with an open window beside it (#222).
 */
interface SecureStore {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
}
