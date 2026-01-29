package com.hanmaum.dn.mobile.di

import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListViewModel
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val appModule = module {
    single<AnnouncementRepository> {
        AnnouncementRepositoryImpl()
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
}