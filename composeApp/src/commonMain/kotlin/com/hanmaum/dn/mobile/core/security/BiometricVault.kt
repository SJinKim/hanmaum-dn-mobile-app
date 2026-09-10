package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable

/** Outcome of sealing into, or opening, the [BiometricVault]. */
sealed interface VaultResult {
    /** [value] is empty for a seal, and the secret for an open. */
    data class Success(val value: String) : VaultResult

    /** The member dismissed the prompt. Not a failure — offer it again. */
    data object Cancelled : VaultResult

    /**
     * Biometrics were re-enrolled since the secret was sealed, so the OS threw
     * the key away. Its own case because the answer is "set Face ID up again",
     * not "try once more" — and a silent retry loop is what this would be
     * otherwise.
     */
    data object Invalidated : VaultResult

    /** No biometric hardware, or nothing enrolled. */
    data object Unavailable : VaultResult

    /** Nothing sealed. */
    data object Empty : VaultResult

    data object Failed : VaultResult
}

/**
 * A secret the operating system releases only against a biometric check.
 *
 * The distinction from [BiometricAuthenticator] is the whole point. That one
 * answers a question — "was this the right face?" — and the app decides what to
 * do with the answer, which means a hooked build can answer it for us. Here the
 * OS holds the key: on iOS the Keychain item carries
 * `kSecAccessControlBiometryCurrentSet`, on Android the Keystore key is created
 * with `setUserAuthenticationRequired(true)`. Without a real biometric match
 * there is no plaintext to hand back, faked boolean or not (#200).
 *
 * What is sealed is the refresh token, never a password: it is revocable, it
 * rotates on use, and it cannot be replayed anywhere but this app's token
 * endpoint.
 */
interface BiometricVault {

    /** True only when *strong* (Class 3) biometrics are present and enrolled. */
    fun isAvailable(): Boolean

    /** True when a secret is sealed — checked without prompting. */
    fun hasSecret(): Boolean

    /** Seals [secret], replacing whatever was there. Prompts to confirm. */
    suspend fun seal(secret: String, title: String, subtitle: String, cancelLabel: String): VaultResult

    /** Prompts, and on success returns the sealed secret. */
    suspend fun open(title: String, subtitle: String, cancelLabel: String): VaultResult

    /**
     * Replaces the secret using the authorisation of the [open] that just ran,
     * without a second prompt.
     *
     * It exists because the sealed secret is a refresh token, and a refresh
     * token is spent the moment it is used: without writing the rotated one
     * back, Face ID works exactly once and then never again (#212). The window
     * has to span a network round trip, which is why this is not folded into
     * [open] — the new token does not exist yet when [open] returns.
     *
     * Only a *write* is authorised this way, never a read. Overwriting the
     * vault with a token the caller already holds discloses nothing, so the
     * property that matters — plaintext leaves the vault only against a live
     * biometric match — is untouched.
     *
     * Returns [VaultResult.Failed] when no recent [open] backs the call.
     */
    suspend fun reseal(secret: String): VaultResult

    /** Forgets the secret. Never prompts — used on logout and teardown. */
    fun clear()
}

/** Creates a [BiometricVault] bound to the current platform UI context. */
@Composable
expect fun rememberBiometricVault(): BiometricVault
