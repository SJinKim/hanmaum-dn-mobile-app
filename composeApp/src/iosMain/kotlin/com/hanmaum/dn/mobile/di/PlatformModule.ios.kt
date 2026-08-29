package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.IosGeofenceManager
import com.hanmaum.dn.mobile.core.notification.IosNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.core.security.IosSecureStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import org.koin.dsl.module

actual val platformModule = module {
    single<SecureStore> { IosSecureStore() }
    single<GeofenceManager> { IosGeofenceManager() }
    single<NotificationService> { IosNotificationService(get()) }
}
