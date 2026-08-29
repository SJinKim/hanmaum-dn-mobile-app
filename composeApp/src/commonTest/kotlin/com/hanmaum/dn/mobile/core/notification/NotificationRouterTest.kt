package com.hanmaum.dn.mobile.core.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationRouterTest {

    @Test
    fun holdsTheTapUntilSomethingCollectsIt() {
        // Cold start: the tap lands before the NavHost exists.
        val router = NotificationRouter()
        router.onNotificationTapped(NotificationDestination.Attendance)

        assertEquals(NotificationDestination.Attendance, router.pending.value)
    }

    @Test
    fun consumedTapDoesNotFireAgain() {
        val router = NotificationRouter()
        router.onNotificationTapped(NotificationDestination.Attendance)
        router.consume()

        assertNull(router.pending.value, "navigating back would re-trigger the destination")
    }
}
