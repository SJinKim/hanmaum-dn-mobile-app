package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable

/** Result of attempting to unlock (read) the gated refresh token. */
sealed interface UnlockResult {
    data class Success(val token: String) : UnlockResult
    data object Cancelled : UnlockResult
    data object Failed : UnlockResult
    /** No token was stored (e.g. fresh install) — caller should route to login. */
    data object Empty : UnlockResult
}

/** Shows the OS biometric/passcode prompt and, on success, returns the decrypted token. */
expect class RefreshTokenUnlocker {
    suspend fun unlock(reason: String): UnlockResult
}

/** Obtain an unlocker bound to the current platform UI context. */
@Composable
expect fun rememberRefreshTokenUnlocker(): RefreshTokenUnlocker
