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

    override fun isNotificationPermissionGranted(): Boolean {
        var granted = false
        center.getNotificationSettingsWithCompletionHandler { settings ->
            granted = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
        }
        return granted
    }

    override fun showAttendanceNotification() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { authorized, _ ->
            if (!authorized) return@requestAuthorizationWithOptions

            val content = UNMutableNotificationContent().apply {
                setTitle("교회에 도착하셨습니다 ⛪")
                setBody("출석 체크를 해주세요!")
            }

            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "attendance_arrival",
                content = content,
                trigger = null,
            )

            center.addNotificationRequest(request) { _ -> }
        }
    }
}
