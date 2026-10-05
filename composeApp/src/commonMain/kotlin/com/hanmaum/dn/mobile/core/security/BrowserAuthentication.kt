package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable

/** External user agent only. null means the member closed it; URLs are never logged. */
interface BrowserAuthentication {
    suspend fun authenticate(url: String, callbackScheme: String): String?
}

@Composable
expect fun rememberBrowserAuthentication(): BrowserAuthentication

internal expect fun secureRandomBytes(size: Int): ByteArray
internal expect fun sha256(bytes: ByteArray): ByteArray
