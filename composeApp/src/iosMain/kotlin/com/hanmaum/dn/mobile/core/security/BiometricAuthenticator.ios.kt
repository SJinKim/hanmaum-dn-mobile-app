package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class BiometricAuthenticator {

    actual fun isAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)

    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): BiometricResult {
        val context = LAContext()
        context.localizedCancelTitle = cancelLabel
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
            return BiometricResult.UNAVAILABLE
        }
        return suspendCancellableCoroutine { cont ->
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = subtitle,
            ) { success, error ->
                val outcome = when {
                    success -> BiometricResult.SUCCESS
                    error?.code == LAErrorUserCancel -> BiometricResult.CANCELLED
                    else -> BiometricResult.FAILED
                }
                if (cont.isActive) cont.resume(outcome)
            }
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { BiometricAuthenticator() }
