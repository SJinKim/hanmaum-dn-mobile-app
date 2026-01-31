package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeViewModel
import com.hanmaum.dn.mobile.features.login.data.repository.AuthRepositoryImpl
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import com.hanmaum.dn.mobile.features.login.presentation.RegisterViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val appModule = module {
    single<AnnouncementRepository> {
        AnnouncementRepositoryImpl()
    }
    single<AuthRepository> {
        AuthRepositoryImpl()
    }


    // Home VM
    viewModel { (token: String) ->
        HomeViewModel(
            token = token,
            repository = get()
        )
    }

    // Detail VM
    viewModel { (token: String, announcementId: String) ->
        AnnouncementDetailViewModel(
            token = token,
            announcementId = announcementId,
            repository = get()
        )
    }

    // List VM
    viewModel { (token: String) ->
        AnnouncementListViewModel(
            token = token,
            repository = get()
        )
    }

    // Register VM
    viewModel { RegisterViewModel(authRepository = get()) }
}