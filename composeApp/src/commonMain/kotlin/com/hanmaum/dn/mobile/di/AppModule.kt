package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.data.repository.LocaleRepositoryImpl
import com.hanmaum.dn.mobile.core.data.repository.AttendancePreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.LocationPreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.ThemeRepositoryImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.repository.LocaleRepository
import com.hanmaum.dn.mobile.core.domain.repository.AttendancePreferences
import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.network.createHttpClient
import com.russhwolf.settings.Settings
import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeViewModel
import com.hanmaum.dn.mobile.core.data.repository.EventRsvpPreferencesImpl
import com.hanmaum.dn.mobile.core.domain.repository.EventRsvpPreferences
import com.hanmaum.dn.mobile.features.attendance.data.repository.AttendanceRepositoryImpl
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceViewModel
import com.hanmaum.dn.mobile.features.events.data.repository.EventRsvpRepositoryImpl
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
import com.hanmaum.dn.mobile.features.geofence.data.repository.ChurchLocationRepositoryImpl
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import com.hanmaum.dn.mobile.features.login.data.repository.AuthRepositoryImpl
import com.hanmaum.dn.mobile.features.login.data.repository.CityLookupRepositoryImpl
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.login.domain.repository.CityLookupRepository
import com.hanmaum.dn.mobile.features.login.presentation.LoginViewModel
import com.hanmaum.dn.mobile.features.login.presentation.RegisterViewModel
import com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImpl
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.notification.data.repository.NotificationRepositoryImpl
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import com.hanmaum.dn.mobile.features.notification.presentation.NotificationListViewModel
import com.hanmaum.dn.mobile.features.ministry.data.repository.MinistryRepositoryImpl
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailViewModel
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListViewModel
import com.hanmaum.dn.mobile.features.album.data.repository.AlbumCacheRepositoryImpl
import com.hanmaum.dn.mobile.features.album.data.repository.AlbumDetailRepositoryImpl
import com.hanmaum.dn.mobile.features.album.data.repository.AlbumsRepositoryImpl
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumCacheRepository
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumDetailRepository
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumsRepository
import com.hanmaum.dn.mobile.features.album.presentation.AlbumDetailViewModel
import com.hanmaum.dn.mobile.features.album.presentation.albums.AlbumsViewModel
import com.hanmaum.dn.mobile.features.calendar.data.repository.CalendarRepositoryImpl
import com.hanmaum.dn.mobile.features.calendar.domain.repository.CalendarRepository
import com.hanmaum.dn.mobile.features.calendar.presentation.CalendarViewModel
import com.hanmaum.dn.mobile.features.floorplan.data.repository.FloorPlanRepositoryImpl
import com.hanmaum.dn.mobile.features.floorplan.domain.repository.FloorPlanRepository
import com.hanmaum.dn.mobile.features.floorplan.presentation.FloorPlanViewModel
import com.hanmaum.dn.mobile.features.pending.presentation.PendingViewModel
import com.hanmaum.dn.mobile.features.pending.presentation.SplashViewModel
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val appModule = module {
    // Repositories
    single<AnnouncementRepository> { AnnouncementRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<CityLookupRepository> { CityLookupRepositoryImpl(get()) }
    single<MemberRepository> { MemberRepositoryImpl(get()) }
    single { createHttpClient(get()) } // Client
    single<TokenStorage> { TokenStorageImpl(Settings()) }
    single<LocaleRepository> { LocaleRepositoryImpl(Settings()) }
    single<ThemeRepository> { ThemeRepositoryImpl(Settings()) }
    single<LocationPreferences> { LocationPreferencesImpl(Settings()) }
    single<AttendancePreferences> { AttendancePreferencesImpl(Settings()) }
    single { CredentialStore(get()) }

    //Splash VM
    viewModel { SplashViewModel(get(), get(), get(), get()) }


    // Home VM
    viewModel { HomeViewModel(repository = get(), notificationRepository = get()) }

    // Detail VM
    viewModel { (announcementId: String) ->
        AnnouncementDetailViewModel(
            announcementId = announcementId,
            repository = get()
        )
    }

    // Pending VM
    viewModel { PendingViewModel(get(), get()) }

    // List VM
    viewModel { AnnouncementListViewModel(get()) }

    // Register VM
    viewModel { RegisterViewModel(get(), get(), get()) }

    // Login VM
    viewModel { LoginViewModel(get(), get(), get(), get(), get()) }

    // Profile VM
    viewModel { ProfileViewModel(get(), get(), get()) }

    // Ministry
    single<MinistryRepository> { MinistryRepositoryImpl(get()) }
    viewModel { MinistryListViewModel(get()) }
    viewModel { (publicId: String) -> MinistryDetailViewModel(publicId, get()) }

    // Attendance
    single<AttendanceRepository> { AttendanceRepositoryImpl(get()) }
    viewModel { AttendanceViewModel(get(), get()) }

    // Event RSVP
    single<EventRsvpRepository> { EventRsvpRepositoryImpl(get()) }
    single<EventRsvpPreferences> { EventRsvpPreferencesImpl(Settings()) }
    viewModel { EventRsvpViewModel(get(), get()) }

    // Geofence
    single<ChurchLocationRepository> { ChurchLocationRepositoryImpl(get()) }
    single { GeofenceCoordinator(get(), get(), get(), get(), get()) }

    // FloorPlan
    single<FloorPlanRepository> { FloorPlanRepositoryImpl(get()) }
    viewModel { FloorPlanViewModel(get()) }

    // Album
    single<AlbumDetailRepository> { AlbumDetailRepositoryImpl(get()) }
    single<AlbumsRepository> { AlbumsRepositoryImpl(get()) }
    single<AlbumCacheRepository> { AlbumCacheRepositoryImpl(Settings()) }
    viewModel { AlbumsViewModel(get(), get(), get()) }
    viewModel { (pcloudCode: String, albumName: String) ->
        AlbumDetailViewModel(pcloudCode = pcloudCode, albumName = albumName, repository = get())
    }

    // Calendar
    single<CalendarRepository> { CalendarRepositoryImpl(get()) }
    viewModel { CalendarViewModel(get()) }

    // Notification
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    viewModel { NotificationListViewModel(get()) }
}
