package com.hanmaum.dn.mobile.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hanmaum.dn.mobile.MainActivity
import com.hanmaum.dn.mobile.core.domain.repository.LocaleRepository
import com.hanmaum.dn.mobile.core.i18n.appStringsFor
import org.koin.android.ext.android.inject

internal const val ANNOUNCEMENT_CHANNEL_ID = "announcements"
internal const val PUSH_DATA_PREFIX = "push_"

class DnFirebaseMessagingService : FirebaseMessagingService() {

    // Instantiated by the system, not by Koin, so the repository is pulled in.
    private val localeRepository: LocaleRepository by inject()

    override fun onNewToken(token: String) {
        PushEventBus.tokenRefreshes.tryEmit(token)
    }

    // Called for foreground messages only (background notification+data messages
    // are rendered by FCM and delivered as launcher-intent extras on tap).
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        ensureAnnouncementChannel(this, localeRepository)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            message.data.forEach { (k, v) -> putExtra(PUSH_DATA_PREFIX + k, v) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            message.data["notificationPublicId"]?.hashCode() ?: 0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, ANNOUNCEMENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(this).notify((message.messageId ?: title).hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between grant check and notify.
        }
    }
}

// Re-registering an existing channel only renames it, so the name follows a language change.
internal fun ensureAnnouncementChannel(context: Context, localeRepository: LocaleRepository) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = appStringsFor(localeRepository.getLocale()).announcementChannelName
        manager.createNotificationChannel(
            NotificationChannel(ANNOUNCEMENT_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH),
        )
    }
}
