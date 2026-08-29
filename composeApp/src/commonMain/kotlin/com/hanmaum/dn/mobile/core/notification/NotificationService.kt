package com.hanmaum.dn.mobile.core.notification

interface NotificationService {
    /**
     * Whether the OS currently lets the app post alerts.
     *
     * Suspending because iOS only exposes the authorization status through an
     * async callback — a synchronous signature there can only guess, and the
     * guess was previously a hardcoded `true`.
     */
    suspend fun isNotificationPermissionGranted(): Boolean

    /**
     * Posts a local notification prompting the user to check in. Asks for
     * notification authorization first on platforms where that can be done
     * without an Activity (iOS); a no-op when permission is denied.
     */
    suspend fun showAttendanceNotification()
}
