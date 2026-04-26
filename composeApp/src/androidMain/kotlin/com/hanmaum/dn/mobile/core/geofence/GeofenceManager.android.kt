package com.hanmaum.dn.mobile.core.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation

class AndroidGeofenceManager(private val context: Context) : GeofenceManager {

    private val client = LocationServices.getGeofencingClient(context)

    override fun isLocationPermissionGranted(): Boolean {
        // TODO: On API 29+, ACCESS_BACKGROUND_LOCATION must also be granted for reliable
        //  background geofencing. Runtime request for background location is handled in
        //  the permission rationale UI (Task 8). This method only gates initial registration.
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {
        if (!isLocationPermissionGranted()) return
        // onEnter is not used on Android — geofence events are delivered via
        // GeofenceBroadcastReceiver which calls GeofenceCoordinator.notifyEntry() directly.

        val geofence = Geofence.Builder()
            .setRequestId("church_geofence")
            .setCircularRegion(location.latitude, location.longitude, location.radiusMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        client.addGeofences(request, buildPendingIntent())
    }

    override fun stopMonitoring() {
        client.removeGeofences(buildPendingIntent())
    }

    private fun buildPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, GeofenceBroadcastReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
