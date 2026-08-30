package com.hanmaum.dn.mobile.core.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a tapped notification wants to take the user. */
enum class NotificationDestination { Attendance, Rsvp }

/**
 * Carries a notification tap from the platform entry point (Android's Activity
 * intent, iOS's UNUserNotificationCenter delegate) to the NavHost.
 *
 * It has to be collected at the `App` root rather than inside a screen: the tap
 * arrives whatever the member was last looking at, and a collector living in
 * HomeScreen only runs while Home is composed.
 *
 * A StateFlow rather than an event stream, because on a cold start the tap is
 * delivered before the NavHost exists — the pending destination has to sit
 * there until something is around to act on it.
 */
class NotificationRouter {

    private val _pending = MutableStateFlow<NotificationDestination?>(null)
    val pending: StateFlow<NotificationDestination?> = _pending.asStateFlow()

    fun onNotificationTapped(destination: NotificationDestination) {
        _pending.value = destination
    }

    /** Clears the pending tap so navigating back doesn't re-trigger it. */
    fun consume() {
        _pending.value = null
    }
}
