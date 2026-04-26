package com.hanmaum.dn.mobile.core.geofence

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLRegion
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject

private const val REGION_ID = "church_geofence"

class IosGeofenceManager : GeofenceManager {

    private val manager = CLLocationManager()

    @Volatile
    private var onEnterCallback: (() -> Unit)? = null

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didEnterRegion: CLRegion) {
            if (didEnterRegion.identifier == REGION_ID) {
                onEnterCallback?.invoke()
            }
        }
    }

    init {
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    override fun isLocationPermissionGranted(): Boolean {
        val status = manager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedAlways ||
               status == kCLAuthorizationStatusAuthorizedWhenInUse
    }

    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {
        onEnterCallback = onEnter
        manager.requestAlwaysAuthorization()

        val region = CLCircularRegion(
            center = CLLocationCoordinate2DMake(location.latitude, location.longitude),
            radius = location.radiusMeters,
            identifier = REGION_ID,
        )
        region.notifyOnEntry = true
        region.notifyOnExit = false
        manager.startMonitoringForRegion(region)
    }

    override fun stopMonitoring() {
        manager.monitoredRegions.filterIsInstance<CLCircularRegion>()
            .filter { it.identifier == REGION_ID }
            .forEach { manager.stopMonitoringForRegion(it) }
        onEnterCallback = null
    }
}
