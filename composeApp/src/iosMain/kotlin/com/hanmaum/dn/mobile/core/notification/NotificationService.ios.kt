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

    override fun showAttendanceNotification() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { authorized, _ ->
            if (!authorized) return@requestAuthorizationWithOptions
            postNotification()
        }
    }

    private fun postNotification() {
        val content = UNMutableNotificationContent().apply {
            setTitle("교회에 도착하셨습니다 ⛪")
            setBody("출석 체크를 해주세요!")
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
