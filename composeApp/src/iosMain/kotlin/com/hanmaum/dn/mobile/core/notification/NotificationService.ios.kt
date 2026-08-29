package com.hanmaum.dn.mobile.core.notification

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

private const val ATTENDANCE_ID = "attendance_arrival"

@OptIn(ExperimentalForeignApi::class)
class IosNotificationService(
    private val router: NotificationRouter,
) : NotificationService {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    private val delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {

        /** The tap. Routed centrally so it works from whatever screen is open. */
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit,
        ) {
            if (didReceiveNotificationResponse.notification.request.identifier == ATTENDANCE_ID) {
                router.onNotificationTapped(NotificationDestination.Attendance)
            }
            withCompletionHandler()
        }

        /**
         * Without this, iOS suppresses the banner while the app is in the
         * foreground — which is exactly when the geofence tends to fire, as the
         * member walks in with the app open.
         */
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
        ) {
            withCompletionHandler(
                UNNotificationPresentationOptionBanner or
                    UNNotificationPresentationOptionList or
                    UNNotificationPresentationOptionSound
            )
        }
    }

    init {
        center.delegate = delegate
    }

    /**
     * Reads the real authorization status instead of assuming it. A status of
     * `notDetermined` counts as *not* granted: the user has never been asked,
     * so an alert posted now would be dropped silently.
     */
    override suspend fun isNotificationPermissionGranted(): Boolean =
        when (currentStatus()) {
            UNAuthorizationStatusAuthorized,
            UNAuthorizationStatusProvisional,
            UNAuthorizationStatusEphemeral -> true
            else -> false
        }

    override suspend fun showAttendanceNotification() {
        // `notDetermined` is the one state worth prompting from — the geofence
        // fired, so the alert is about to be useful and the ask has context.
        // Once the user has answered, iOS returns the stored answer without
        // showing anything, and a denial simply stops us here.
        val granted = if (currentStatus() == UNAuthorizationStatusNotDetermined) {
            requestAuthorization()
        } else {
            isNotificationPermissionGranted()
        }
        if (!granted) return

        val content = UNMutableNotificationContent().apply {
            setTitle("교회에 도착하셨습니다 ⛪")
            setBody("출석 체크를 해주세요!")
        }

        // trigger = null means deliver immediately (on geofence arrival)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = ATTENDANCE_ID,
            content = content,
            trigger = null,
        )

        center.addNotificationRequest(request) { _ -> }
    }

    private suspend fun currentStatus(): Long =
        suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings: UNNotificationSettings? ->
                continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
            }
        }

    private suspend fun requestAuthorization(): Boolean =
        suspendCancellableCoroutine { continuation ->
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { authorized, _ ->
                continuation.resume(authorized)
            }
        }
}
