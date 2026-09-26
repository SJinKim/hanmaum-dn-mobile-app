package com.hanmaum.dn.mobile.core.geofence

import android.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeofencePermissionsTest {

    @Test
    fun android13BatchAsksNotificationsWithoutBackgroundLocation() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS),
            foregroundPermissions(33),
        )
        assertTrue(needsSeparateBackgroundRequest(33))
    }

    @Test
    fun android11BatchLeavesBackgroundLocationForItsOwnRequest() {
        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), foregroundPermissions(30))
        assertTrue(needsSeparateBackgroundRequest(30))
    }

    @Test
    fun android10StillAsksBackgroundLocationInTheSameBatch() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            foregroundPermissions(29),
        )
        assertFalse(needsSeparateBackgroundRequest(29))
    }

    @Test
    fun olderDevicesAskFineLocationOnly() {
        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), foregroundPermissions(24))
        assertFalse(needsSeparateBackgroundRequest(24))
    }
}
