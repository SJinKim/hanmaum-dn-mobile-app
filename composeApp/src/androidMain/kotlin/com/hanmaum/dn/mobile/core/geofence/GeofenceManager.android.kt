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
    private var onEnterCallback: (() -> Unit)? = null

    override fun isLocationPermissionGranted(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {
        if (!isLocationPermissionGranted()) return
        onEnterCallback = onEnter

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
        onEnterCallback = null
    }

    /** Called by [GeofenceBroadcastReceiver] when the OS fires the entry event. */
    fun triggerEntry() {
        onEnterCallback?.invoke()
    }

    private fun buildPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, GeofenceBroadcastReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
