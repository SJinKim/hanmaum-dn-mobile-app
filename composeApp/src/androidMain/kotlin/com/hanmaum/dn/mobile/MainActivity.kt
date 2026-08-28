package com.hanmaum.dn.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.hanmaum.dn.mobile.core.push.PUSH_DATA_PREFIX
import com.hanmaum.dn.mobile.core.push.PushEventBus
import com.hanmaum.dn.mobile.core.push.parsePushTap

// FragmentActivity (extends ComponentActivity) is required by androidx.biometric's
// BiometricPrompt. setContent / enableEdgeToEdge are unaffected.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handlePushExtras(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePushExtras(intent)
    }

    private fun handlePushExtras(intent: Intent?) {
        val extras = intent?.extras ?: return
        // Foreground pushes route through DnFirebaseMessagingService with prefixed keys.
        val data = extras.keySet()
            .filter { it.startsWith(PUSH_DATA_PREFIX) }
            .associate { it.removePrefix(PUSH_DATA_PREFIX) to (extras.getString(it) ?: "") }
        // Background-delivered FCM taps put data keys directly on the launcher intent.
        val direct = listOf("type", "referenceType", "referencePublicId", "notificationPublicId")
            .mapNotNull { key -> extras.getString(key)?.let { key to it } }
            .toMap()
        parsePushTap(data.ifEmpty { direct })?.let { PushEventBus.notificationTaps.tryEmit(it) }
    }
}
