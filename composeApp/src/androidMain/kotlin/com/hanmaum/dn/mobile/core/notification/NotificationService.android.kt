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

private const val CHANNEL_ID = "attendance_channel"
private const val NOTIFICATION_ID = 1001

/** Read back by MainActivity to route the tap through [NotificationRouter]. */
const val EXTRA_DESTINATION = "com.hanmaum.dn.mobile.NOTIFICATION_DESTINATION"
const val DESTINATION_ATTENDANCE = "attendance"

class AndroidNotificationService(private val context: Context) : NotificationService {

    init { createChannel() }

    override suspend fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override suspend fun showAttendanceNotification() {
        if (!isNotificationPermissionGranted()) return

        // SINGLE_TOP, not CLEAR_TASK: the app is usually already running when
        // the geofence fires, and clearing the task threw away the user's back
        // stack. The extra tells MainActivity where the tap wants to go.
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_ATTENDANCE)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("교회에 도착하셨습니다 ⛪")
            .setContentText("출석 체크를 해주세요!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "출석 알림", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "교회 도착 시 출석 체크 알림" }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
