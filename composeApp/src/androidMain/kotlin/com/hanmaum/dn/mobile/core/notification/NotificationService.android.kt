package com.hanmaum.dn.mobile.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hanmaum.dn.mobile.MainActivity
import com.hanmaum.dn.mobile.core.domain.repository.LocaleRepository
import com.hanmaum.dn.mobile.core.i18n.appStringsFor

private const val CHANNEL_ID = "attendance_channel"
private const val NOTIFICATION_ID = 1001

class AndroidNotificationService(
    private val context: Context,
    private val localeRepository: LocaleRepository,
) : NotificationService {

    init { createChannel() }

    override fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun showAttendanceNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Re-registering an existing channel only renames it, so this follows a language change.
        createChannel()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission can be revoked between the check above and notify().
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val strings = appStringsFor(localeRepository.getLocale())
            val channel = NotificationChannel(
                CHANNEL_ID, strings.attendanceChannelName, NotificationManager.IMPORTANCE_HIGH
            ).apply { description = strings.attendanceChannelDescription }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
