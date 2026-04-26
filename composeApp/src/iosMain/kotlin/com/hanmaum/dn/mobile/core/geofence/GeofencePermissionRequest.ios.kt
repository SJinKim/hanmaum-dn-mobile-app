package com.hanmaum.dn.mobile.core.geofence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun GeofencePermissionRequest(onResult: (Boolean) -> Unit) {
    // iOS: CLLocationManager.requestAlwaysAuthorization() is called inside
    // IosGeofenceManager.startMonitoring(). Optimistically proceed here.
    LaunchedEffect(Unit) { onResult(true) }
}
