package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import org.koin.dsl.module

// TODO: replaced with IosGeofenceManager + IosNotificationService in Task 7
actual val platformModule = module {
    single<GeofenceManager> {
        object : GeofenceManager {
            override fun isLocationPermissionGranted() = false
            override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {}
            override fun stopMonitoring() {}
        }
    }
    single<NotificationService> {
        object : NotificationService {
            override fun isNotificationPermissionGranted() = false
            override fun showAttendanceNotification() {}
        }
    }
}
