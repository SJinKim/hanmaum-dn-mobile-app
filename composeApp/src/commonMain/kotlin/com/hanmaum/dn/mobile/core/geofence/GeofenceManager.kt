package com.hanmaum.dn.mobile.core.geofence

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation

interface GeofenceManager {
    /** Returns true if ACCESS_FINE_LOCATION (Android) or whenInUse (iOS) is granted. */
    fun isLocationPermissionGranted(): Boolean

    /**
     * Registers the geofence with the OS.
     * [onEnter] is invoked (on any thread) when the user enters the radius.
     * Safe to call multiple times — re-registers if already active.
     */
    fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit)

    fun stopMonitoring()
}
