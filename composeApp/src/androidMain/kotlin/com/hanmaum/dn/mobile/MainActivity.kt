package com.hanmaum.dn.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.hanmaum.dn.mobile.core.notification.DESTINATION_ATTENDANCE
import com.hanmaum.dn.mobile.core.notification.EXTRA_DESTINATION
import com.hanmaum.dn.mobile.core.notification.NotificationDestination
import com.hanmaum.dn.mobile.core.notification.NotificationRouter
import org.koin.android.ext.android.inject

/**
 * FragmentActivity rather than ComponentActivity: BiometricPrompt requires a
 * FragmentActivity host, and the Face ID / fingerprint setting needs it.
 */
class MainActivity : FragmentActivity() {

    private val notificationRouter: NotificationRouter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Cold start from a notification tap: the router replays it once the
        // NavHost is composed.
        routeNotificationTap(intent)

        setContent {
            App()
        }
    }

    /** Warm start — the launch intent is delivered here, not to onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeNotificationTap(intent)
    }

    private fun routeNotificationTap(intent: Intent?) {
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: return
        // Consume it, so a configuration change doesn't replay the navigation.
        intent.removeExtra(EXTRA_DESTINATION)
        if (destination == DESTINATION_ATTENDANCE) {
            notificationRouter.onNotificationTapped(NotificationDestination.Attendance)
        }
    }
}
