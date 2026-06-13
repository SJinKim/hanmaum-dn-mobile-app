package com.hanmaum.dn.mobile.core.security

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.coroutines.resume

actual class RefreshTokenUnlocker(
    private val activity: FragmentActivity,
    private val store: BiometricRefreshStore,
) {
    actual suspend fun unlock(reason: String): UnlockResult = withContext(Dispatchers.Main) {
        if (!store.hasStored()) return@withContext UnlockResult.Empty
        val cipher = runCatching { store.cipherForUnlock() }.getOrNull()
            ?: return@withContext UnlockResult.Failed
        suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val authedCipher = result.cryptoObject?.cipher
                        val token = authedCipher?.let { runCatching { store.decryptAfterAuth(it) }.getOrNull() }
                        if (cont.isActive) {
                            cont.resume(if (token != null) UnlockResult.Success(token) else UnlockResult.Failed)
                        }
                    }

                    override fun onAuthenticationError(code: Int, msg: CharSequence) {
                        val cancelled = code == BiometricPrompt.ERROR_USER_CANCELED ||
                            code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            code == BiometricPrompt.ERROR_CANCELED
                        if (cont.isActive) {
                            cont.resume(if (cancelled) UnlockResult.Cancelled else UnlockResult.Failed)
                        }
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(reason)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                // NOTE: do NOT call setNegativeButtonText — the API forbids it when DEVICE_CREDENTIAL is allowed
                .build()
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        }
    }
}

@Composable
actual fun rememberRefreshTokenUnlocker(): RefreshTokenUnlocker {
    val activity = LocalActivity.current as FragmentActivity
    val store = koinInject<BiometricRefreshStore>()
    return remember { RefreshTokenUnlocker(activity, store) }
}
