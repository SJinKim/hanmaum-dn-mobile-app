package com.hanmaum.dn.mobile.core.security

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }
internal actual fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

/** Single foreground browser transaction, lost safely on process death. */
internal object AndroidBrowserSession {
    var pending: CancellableContinuation<String?>? = null
    var url: String? = null

    fun finish(callback: String? = null) {
        val continuation = pending
        pending = null
        url = null
        if (continuation?.isActive == true) continuation.resume(callback)
    }
}

/** Trampoline tracks leaving/returning from the actual browser, not a timer. */
class BrowserLoginActivity : Activity() {
    private var launched = false
    private var leftForBrowser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launched = savedInstanceState?.getBoolean("launched") ?: false
        leftForBrowser = savedInstanceState?.getBoolean("left") ?: false
        if (intent.action == Intent.ACTION_VIEW) {
            acceptCallback(intent)
        } else if (AndroidBrowserSession.pending == null) {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        acceptCallback(intent)
    }

    private fun acceptCallback(intent: Intent) {
        // The shared transaction verifies exact URI, issuer, state and duplicate params.
        AndroidBrowserSession.finish(intent.dataString)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) return
        if (launched && leftForBrowser) {
            AndroidBrowserSession.finish()
            finish()
        } else if (!launched) {
            val url = AndroidBrowserSession.url ?: return finish()
            launched = true
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
            } catch (_: Exception) {
                val continuation = AndroidBrowserSession.pending
                AndroidBrowserSession.pending = null
                AndroidBrowserSession.url = null
                if (continuation?.isActive == true) continuation.resumeWithException(IllegalStateException("Browser unavailable"))
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (launched) leftForBrowser = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("launched", launched)
        outState.putBoolean("left", leftForBrowser)
        super.onSaveInstanceState(outState)
    }
}

@Composable
actual fun rememberBrowserAuthentication(): BrowserAuthentication {
    val activity = requireNotNull(LocalActivity.current)
    return remember(activity) {
        object : BrowserAuthentication {
            override suspend fun authenticate(url: String, callbackScheme: String): String? =
                suspendCancellableCoroutine { continuation ->
                    check(AndroidBrowserSession.pending == null) { "Browser login already running" }
                    AndroidBrowserSession.pending = continuation
                    AndroidBrowserSession.url = url
                    continuation.invokeOnCancellation {
                        if (AndroidBrowserSession.pending === continuation) {
                            AndroidBrowserSession.pending = null
                            AndroidBrowserSession.url = null
                        }
                    }
                    if (!continuation.isActive) return@suspendCancellableCoroutine
                    try {
                        activity.startActivity(Intent(activity, BrowserLoginActivity::class.java))
                    } catch (_: Exception) {
                        AndroidBrowserSession.pending = null
                        AndroidBrowserSession.url = null
                        continuation.resumeWithException(IllegalStateException("Browser unavailable"))
                    }
                }
        }
    }
}
