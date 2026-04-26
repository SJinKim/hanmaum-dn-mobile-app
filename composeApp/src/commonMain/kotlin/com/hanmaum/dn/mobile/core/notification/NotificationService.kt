package com.hanmaum.dn.mobile.core.notification

interface NotificationService {
    /** Returns true if the user has granted notification permission. */
    fun isNotificationPermissionGranted(): Boolean

    /** Posts a local notification prompting the user to check in. */
    fun showAttendanceNotification()
}
