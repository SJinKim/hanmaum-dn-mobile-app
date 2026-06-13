package com.hanmaum.dn.mobile.core.security

/**
 * Hardware-backed store for the offline refresh token.
 *
 * - [store] / [delete] / [hasStored] are synchronous and require NO user auth, so
 *   silent token rotation can persist a freshly-issued refresh token without ever
 *   prompting (iOS: free Keychain write; Android: RSA public-key encrypt).
 * - The *read* is gated behind device auth (biometric or passcode) and is performed
 *   by [RefreshTokenUnlocker], which owns the platform prompt.
 *
 * On Android the read uses an RSA private key in the Keystore created with
 * `setUserAuthenticationRequired(true)`; [cipherForUnlock] returns the Cipher that
 * `BiometricPrompt` authorizes, and [decryptAfterAuth] finishes the decryption.
 * On iOS the read is a Keychain lookup whose `SecAccessControl` self-prompts, so the
 * Android-only members below are no-op/unused there.
 */

/** The subset of [BiometricRefreshStore] the vault needs; lets tests fake persistence. */
interface RefreshTokenPersistence {
    fun isDeviceSecured(): Boolean
    fun hasStored(): Boolean
    fun store(token: String)
    fun delete()
}

expect class BiometricRefreshStore : RefreshTokenPersistence {
    /** True if a device secret (biometric or passcode) exists so gating is possible. */
    override fun isDeviceSecured(): Boolean

    override fun hasStored(): Boolean

    /** Persist (encrypt) the refresh token. No prompt. */
    override fun store(token: String)

    override fun delete()
}
