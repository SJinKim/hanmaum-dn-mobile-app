package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.createHttpClient
import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeViewModel
import com.hanmaum.dn.mobile.features.attendance.data.repository.AttendanceRepositoryImpl
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceViewModel
import com.hanmaum.dn.mobile.features.login.data.repository.AuthRepositoryImpl
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.login.presentation.LoginViewModel
import com.hanmaum.dn.mobile.features.login.presentation.RegisterViewModel
import com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImpl
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.ministry.data.repository.MinistryRepositoryImpl
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailViewModel
import com.hanmaum.dn.mobile.features.geofence.data.repository.ChurchLocationRepositoryImpl
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListViewModel
import com.hanmaum.dn.mobile.features.pending.presentation.PendingViewModel
import com.hanmaum.dn.mobile.features.pending.presentation.SplashViewModel
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val appModule = module {
    // Repositories
    single<AnnouncementRepository> { AnnouncementRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<MemberRepository> { MemberRepositoryImpl(get()) }
    single { createHttpClient(get()) } // Client
    single<TokenStorage> { TokenStorageImpl() } // Storage

    //Splash VM
    viewModel { SplashViewModel(get(), get()) }


    // Home VM
    viewModel { HomeViewModel(repository = get()) }

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
    viewModel { RegisterViewModel(get(), get()) }

    // Login VM — authRepository, memberRepository, tokenStorage, httpClient
    viewModel { LoginViewModel(get(), get(), get(), get()) }

    // Profile VM
    viewModel { ProfileViewModel(get(), get()) }

    // Ministry
    single<MinistryRepository> { MinistryRepositoryImpl(get()) }
    viewModel { MinistryListViewModel(get()) }
    viewModel { (publicId: String) -> MinistryDetailViewModel(publicId, get()) }

    // Attendance
    single<AttendanceRepository> { AttendanceRepositoryImpl(get()) }
    viewModel { AttendanceViewModel(get()) }

    // Geofence
    single<ChurchLocationRepository> { ChurchLocationRepositoryImpl(get()) }
    single { GeofenceCoordinator(get(), get(), get(), get()) }
}