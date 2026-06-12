package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable

/** Outcome of a biometric prompt. */
enum class BiometricResult {
    SUCCESS,      // user authenticated
    FAILED,       // hardware/error or repeated mismatch
    CANCELLED,    // user dismissed the prompt
    UNAVAILABLE,  // no biometric hardware / nothing enrolled
}

/**
 * UI-gate biometric authenticator. Reveals the already-stored session on success;
 * it does not crypto-bind tokens. Obtain it inside Compose via
 * [rememberBiometricAuthenticator] (Android needs the hosting FragmentActivity).
 */
expect class BiometricAuthenticator {
    /** True only when biometric hardware is present AND something is enrolled. */
    fun isAvailable(): Boolean

    /** Shows the system biometric prompt and suspends until it resolves. */
    suspend fun authenticate(
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): BiometricResult
}

/** Creates a [BiometricAuthenticator] bound to the current platform UI context. */
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
