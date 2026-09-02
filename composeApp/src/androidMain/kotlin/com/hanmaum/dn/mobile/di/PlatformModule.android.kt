package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.AndroidGeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.location.AndroidCurrentLocationProvider
import com.hanmaum.dn.mobile.core.location.CurrentLocationProvider
import com.hanmaum.dn.mobile.core.notification.AndroidNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.core.push.AndroidPushManager
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.core.security.AndroidSecureStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<GeofenceManager> { AndroidGeofenceManager(androidContext()) }
    single<CurrentLocationProvider> { AndroidCurrentLocationProvider(androidContext()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
    single<SecureStore> { AndroidSecureStore(androidContext()) }
    single<PushManager> { AndroidPushManager(androidContext(), get()) }
}
