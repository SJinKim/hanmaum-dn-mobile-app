package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.IosGeofenceManager
import com.hanmaum.dn.mobile.core.location.CurrentLocationProvider
import com.hanmaum.dn.mobile.core.location.IosCurrentLocationProvider
import com.hanmaum.dn.mobile.core.notification.IosNotificationService
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.core.push.IosPushManager
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.core.security.IosBiometricVault
import com.hanmaum.dn.mobile.core.security.IosSecureStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import coil3.PlatformContext
import com.hanmaum.dn.mobile.features.profile.data.image.FileProfileImagePreparer
import com.hanmaum.dn.mobile.features.profile.data.image.IosProfileImageCodec
import com.hanmaum.dn.mobile.features.profile.data.image.buildProfileImageLoader
import com.hanmaum.dn.mobile.features.profile.data.image.profileImageTempDirectory
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparer
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import org.koin.dsl.module

actual val platformModule = module {
    single<GeofenceManager> { IosGeofenceManager() }
    single<CurrentLocationProvider> { IosCurrentLocationProvider() }
    single<NotificationService> { IosNotificationService() }
    single<SecureStore> { IosSecureStore() }
    single<BiometricVault> { IosBiometricVault() }
    single<PushManager> { IosPushManager() }
    // The tmp folder is not backed up; picked originals and prepared files share it.
    single<ProfileImagePreparer> { FileProfileImagePreparer(IosProfileImageCodec(), profileImageTempDirectory()) }
    single {
        val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: NSTemporaryDirectory()
        buildProfileImageLoader(PlatformContext.INSTANCE, get(), caches)
    }
}
