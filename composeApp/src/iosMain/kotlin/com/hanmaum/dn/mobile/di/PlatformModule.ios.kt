package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.data.repository.SecureSettingsFactory
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.IosGeofenceManager
import com.hanmaum.dn.mobile.core.notification.IosNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.russhwolf.settings.Settings
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule = module {
    single<GeofenceManager> { IosGeofenceManager() }
    single<NotificationService> { IosNotificationService() }
    single(named("secure")) { SecureSettingsFactory().create() }
}
