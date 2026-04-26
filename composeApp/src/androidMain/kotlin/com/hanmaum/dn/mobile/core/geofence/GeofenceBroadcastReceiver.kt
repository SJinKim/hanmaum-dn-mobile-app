package com.hanmaum.dn.mobile.core.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import org.koin.core.context.GlobalContext

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        GlobalContext.get().get<GeofenceCoordinator>().notifyEntry()
    }
}
