package com.hanmaum.dn.mobile.core.notification

interface NotificationService {
    /** Returns true if the user has granted notification permission. */
    fun isNotificationPermissionGranted(): Boolean

    /** Posts a local notification prompting the user to check in, with already-localized copy. */
    fun showAttendanceNotification(title: String, body: String)
}
