@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSURL
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { bytes ->
    bytes.usePinned { check(SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) == 0) }
}

internal actual fun sha256(bytes: ByteArray): ByteArray = ByteArray(32).also { digest ->
    bytes.usePinned { input -> digest.usePinned { output ->
        CC_SHA256(input.addressOf(0), bytes.size.convert(), output.addressOf(0).reinterpret())
    } }
}

private class AuthenticationAnchor(private val window: UIWindow) : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): UIWindow =
        window
}

private class IosBrowserAuthentication : BrowserAuthentication {
    private var session: ASWebAuthenticationSession? = null
    // ASWebAuthenticationSession holds its presentation provider weakly.
    private var anchor: AuthenticationAnchor? = null

    override suspend fun authenticate(url: String, callbackScheme: String): String? =
        suspendCancellableCoroutine { continuation ->
            check(session == null) { "Browser login already running" }
            val window = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .flatMap { it.windows.filterIsInstance<UIWindow>() }
                .firstOrNull { it.isKeyWindow() }
            if (window == null) {
                continuation.resumeWithException(IllegalStateException("Browser presentation unavailable"))
                return@suspendCancellableCoroutine
            }
            anchor = AuthenticationAnchor(window)
            val authentication = ASWebAuthenticationSession(
                uRL = NSURL(string = url),
                callbackURLScheme = callbackScheme,
                completionHandler = { callback, error ->
                    session = null
                    anchor = null
                    if (continuation.isActive) {
                        when {
                            callback != null -> continuation.resume(callback.absoluteString)
                            error?.code == ASWebAuthenticationSessionErrorCodeCanceledLogin -> continuation.resume(null)
                            else -> continuation.resumeWithException(IllegalStateException("Browser login failed"))
                        }
                    }
                },
            )
            session = authentication
            authentication.presentationContextProvider = anchor
            continuation.invokeOnCancellation { authentication.cancel(); session = null; anchor = null }
            if (!continuation.isActive) return@suspendCancellableCoroutine
            if (!authentication.start()) {
                session = null
                anchor = null
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Browser unavailable"))
            }
        }
}

@Composable
actual fun rememberBrowserAuthentication(): BrowserAuthentication = remember { IosBrowserAuthentication() }
