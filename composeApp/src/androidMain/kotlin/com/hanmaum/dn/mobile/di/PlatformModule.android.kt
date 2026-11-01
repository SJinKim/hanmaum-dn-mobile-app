package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.AndroidGeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.location.AndroidCurrentLocationProvider
import com.hanmaum.dn.mobile.core.location.CurrentLocationProvider
import com.hanmaum.dn.mobile.core.notification.AndroidNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.core.push.AndroidPushManager
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.core.security.AndroidBiometricVault
import com.hanmaum.dn.mobile.core.security.AndroidSecureStore
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.core.security.SecureStore
import com.hanmaum.dn.mobile.features.profile.data.image.AndroidProfileImageCodec
import com.hanmaum.dn.mobile.features.profile.data.image.FileProfileImagePreparer
import com.hanmaum.dn.mobile.features.profile.data.image.buildProfileImageLoader
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparer
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<GeofenceManager> { AndroidGeofenceManager(androidContext()) }
    single<CurrentLocationProvider> { AndroidCurrentLocationProvider(androidContext()) }
    single<NotificationService> { AndroidNotificationService(androidContext(), get()) }
    single<SecureStore> { AndroidSecureStore(androidContext()) }
    // No activity: this instance exists so logout can clear the vault.
    single<BiometricVault> { AndroidBiometricVault(androidContext()) }
    single<PushManager> { AndroidPushManager(androidContext(), get(), get()) }
    // cacheDir is private and excluded from backup.
    single<ProfileImagePreparer> {
        FileProfileImagePreparer(
            AndroidProfileImageCodec(androidContext()),
            File(androidContext().cacheDir, "profile-image").path,
        )
    }
    single { buildProfileImageLoader(androidContext(), get(), androidContext().cacheDir.path) }
}
