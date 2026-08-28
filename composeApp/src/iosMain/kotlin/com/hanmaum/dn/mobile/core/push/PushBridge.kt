package com.hanmaum.dn.mobile.core.push

// Single writer (Swift main thread); plain var is sufficient.
private var latestFcmToken: String? = null

/** Swift: PushBridgeKt.handlePushToken(token:) from MessagingDelegate. */
fun handlePushToken(token: String) {
    latestFcmToken = token
    PushEventBus.tokenRefreshes.tryEmit(token)
}

/** Swift: PushBridgeKt.handlePushTap(data:) from the UNUserNotificationCenter tap handler. */
fun handlePushTap(data: Map<Any?, *>) {
    val stringData = buildMap {
        data.forEach { (k, v) -> if (k is String && v is String) put(k, v) }
    }
    parsePushTap(stringData)?.let { PushEventBus.notificationTaps.tryEmit(it) }
}

internal fun storedFcmToken(): String? = latestFcmToken
