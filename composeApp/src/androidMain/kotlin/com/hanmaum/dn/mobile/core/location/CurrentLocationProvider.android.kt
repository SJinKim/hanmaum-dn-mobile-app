package com.hanmaum.dn.mobile.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * A fix with no accuracy attached tells us nothing about how wrong it is, so it
 * is treated as very wrong rather than as perfect — [isConfidentlyWithin] then
 * refuses it. FusedLocationProvider always reports accuracy in practice; this
 * only covers the contract allowing it not to.
 */
private const val UNKNOWN_ACCURACY_METERS = 10_000.0

class AndroidCurrentLocationProvider(private val context: Context) : CurrentLocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) return LocationResult.PermissionDenied
        if (!locationServicesEnabled()) return LocationResult.LocationServicesDisabled

        val tokenSource = CancellationTokenSource()
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestFix(tokenSource) } ?: LocationResult.Timeout
        } finally {
            tokenSource.cancel()
        }
    }

    private suspend fun requestFix(tokenSource: CancellationTokenSource): LocationResult =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { tokenSource.cancel() }

            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { location ->
                        if (!continuation.isActive) return@addOnSuccessListener
                        continuation.resume(
                            if (location == null) {
                                LocationResult.Failed("no fix available")
                            } else {
                                LocationResult.Success(
                                    DeviceLocation(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        accuracyMeters = if (location.hasAccuracy()) {
                                            location.accuracy.toDouble()
                                        } else {
                                            UNKNOWN_ACCURACY_METERS
                                        },
                                    ),
                                )
                            },
                        )
                    }
                    .addOnFailureListener { error ->
                        if (!continuation.isActive) return@addOnFailureListener
                        continuation.resume(LocationResult.Failed(error.message))
                    }
            } catch (_: SecurityException) {
                // The permission can be revoked between the check in
                // getCurrentLocation and this call, so the guard has to sit at
                // the platform call itself.
                if (continuation.isActive) continuation.resume(LocationResult.PermissionDenied)
            }
        }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun locationServicesEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }
}
