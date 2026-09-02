package com.hanmaum.dn.mobile.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosCurrentLocationProvider : CurrentLocationProvider {

    private val manager = CLLocationManager()

    private var onResult: ((LocationResult) -> Unit)? = null

    /**
     * CLLocationManager holds its delegate weakly, so this must stay a strong
     * property here — a delegate created inside the request would be collected
     * before the callback arrives and the fix would never be delivered.
     */
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val fix = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            // A negative horizontal accuracy means the coordinate is invalid.
            if (fix.horizontalAccuracy < 0) {
                deliver(LocationResult.Failed("invalid fix"))
                return
            }
            val location = fix.coordinate.useContents {
                DeviceLocation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = fix.horizontalAccuracy,
                )
            }
            deliver(LocationResult.Success(location))
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            deliver(LocationResult.Failed(didFailWithError.localizedDescription))
        }
    }

    init {
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    override suspend fun getCurrentLocation(): LocationResult = withContext(Dispatchers.Main) {
        // CLLocationManager needs a run loop, so everything here stays on main.
        if (!CLLocationManager.locationServicesEnabled()) {
            return@withContext LocationResult.LocationServicesDisabled
        }
        val status = manager.authorizationStatus
        val authorized = status == kCLAuthorizationStatusAuthorizedAlways ||
            status == kCLAuthorizationStatusAuthorizedWhenInUse
        if (!authorized) return@withContext LocationResult.PermissionDenied

        withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestFix() } ?: LocationResult.Timeout
    }

    private suspend fun requestFix(): LocationResult = suspendCancellableCoroutine { continuation ->
        onResult = { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        continuation.invokeOnCancellation { onResult = null }
        // requestLocation delivers exactly one fix, then stops on its own.
        manager.requestLocation()
    }

    private fun deliver(result: LocationResult) {
        val callback = onResult ?: return
        onResult = null
        callback(result)
    }
}
