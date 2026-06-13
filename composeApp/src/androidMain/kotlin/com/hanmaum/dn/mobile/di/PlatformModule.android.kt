package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.data.repository.SecureSettingsFactory
import com.hanmaum.dn.mobile.core.geofence.AndroidGeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.AndroidNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.russhwolf.settings.Settings
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule = module {
    single<GeofenceManager> { AndroidGeofenceManager(androidContext()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
    single(named("secure")) { SecureSettingsFactory(androidContext()).create() }
}
