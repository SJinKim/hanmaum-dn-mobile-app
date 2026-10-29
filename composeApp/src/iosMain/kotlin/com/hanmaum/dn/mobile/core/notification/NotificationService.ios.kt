package com.hanmaum.dn.mobile.core.notification

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationService : NotificationService {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    // On iOS, authorization status can only be checked asynchronously.
    // This method returns true optimistically; the OS handles denial gracefully
    // by silencing the notification. GeofenceCoordinator does not gate on this value.
    override fun isNotificationPermissionGranted(): Boolean = true

    override fun showAttendanceNotification(title: String, body: String) {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { authorized, _ ->
            if (!authorized) return@requestAuthorizationWithOptions
            postNotification(title, body)
        }
    }

    private fun postNotification(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }

        // trigger = null means deliver immediately (on geofence arrival)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "attendance_arrival",
            content = content,
            trigger = null,
        )

        center.addNotificationRequest(request) { _ -> }
    }
}
