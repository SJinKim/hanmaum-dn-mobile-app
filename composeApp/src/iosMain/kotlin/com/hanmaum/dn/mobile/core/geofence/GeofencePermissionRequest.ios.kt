package com.hanmaum.dn.mobile.core.geofence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GeofencePermissionRequest(onResult: (Boolean) -> Unit) {
    val currentOnResult = rememberUpdatedState(onResult)
    val requester = remember {
        IosLocationPermissionRequester { granted -> currentOnResult.value(granted) }
    }

    DisposableEffect(requester) {
        requester.request()
        onDispose { requester.dispose() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosLocationPermissionRequester(
    private val onResult: (Boolean) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private var completed = false
    private var requestStarted = false

    init {
        manager.delegate = this
    }

    fun request() {
        handleAuthorizationStatus(manager)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        handleAuthorizationStatus(manager)
    }

    private fun handleAuthorizationStatus(manager: CLLocationManager) {
        if (completed) return

        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse,
            -> complete(true)

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> complete(false)

            kCLAuthorizationStatusNotDetermined -> {
                if (!requestStarted) {
                    requestStarted = true
                    manager.requestAlwaysAuthorization()
                }
            }
        }
    }

    private fun complete(granted: Boolean) {
        completed = true
        onResult(granted)
    }

    fun dispose() {
        manager.delegate = null
    }
}
