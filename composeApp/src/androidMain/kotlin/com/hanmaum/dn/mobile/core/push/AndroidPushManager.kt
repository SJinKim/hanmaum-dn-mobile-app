package com.hanmaum.dn.mobile.core.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.hanmaum.dn.mobile.core.notification.NotificationService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidPushManager(
    context: Context,
    private val notificationService: NotificationService,
) : PushManager {
    override val platform: String = "ANDROID"

    init {
        // Background pushes are rendered by FCM itself; the channel from the manifest
        // meta-data must already exist or Android falls back to a misc channel.
        ensureAnnouncementChannel(context)
    }

    override suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } catch (_: Exception) {
            // FirebaseApp not initialized - push disabled.
            cont.resume(null)
        }
    }

    override fun isPermissionGranted(): Boolean = notificationService.isNotificationPermissionGranted()

    // The real POST_NOTIFICATIONS dialog needs an Activity and runs through the
    // NotificationPermissionRequest composable; this only reports current state.
    override suspend fun requestPermission(): Boolean = isPermissionGranted()
}
