package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.createHttpClient
import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeViewModel
import com.hanmaum.dn.mobile.features.login.data.repository.AuthRepositoryImpl
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.login.presentation.LoginViewModel
import com.hanmaum.dn.mobile.features.login.presentation.RegisterViewModel
import com.hanmaum.dn.mobile.features.member.data.repository.MemberRepositoryImpl
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
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

    // Login VM
    viewModel { LoginViewModel(get(), get(), get()) }

    // Profile VM
    viewModel { ProfileViewModel(get()) }
}