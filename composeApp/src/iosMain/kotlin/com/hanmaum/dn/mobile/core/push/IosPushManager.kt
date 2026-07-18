package com.hanmaum.dn.mobile.core.push

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time
import kotlin.coroutines.resume

class IosPushManager : PushManager {
    override val platform: String = "IOS"

    // Token arrives via MessagingDelegate in Swift -> PushBridge.handlePushToken.
    override suspend fun currentToken(): String? = storedFcmToken()

    override fun isPermissionGranted(): Boolean {
        // Synchronous best-effort snapshot (max 500ms); authoritative state flows
        // through requestPermission.
        var granted = false
        val semaphore = dispatch_semaphore_create(0)
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            granted = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
            dispatch_semaphore_signal(semaphore)
        }
        dispatch_semaphore_wait(semaphore, dispatch_time(DISPATCH_TIME_NOW, 500_000_000))
        return granted
    }

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
        ) { granted, _ -> cont.resume(granted) }
    }
}
