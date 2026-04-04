package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.BottomTab
import com.hanmaum.dn.mobile.core.presentation.components.ChurchBottomBar
import com.hanmaum.dn.mobile.core.presentation.components.ChurchTopBar
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.presentation.components.HeroBannerSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.LatestNewsSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.QuickMenuSection
import org.koin.compose.viewmodel.koinViewModel

data class BannerItem(val title: String, val color: Color)
data class NewsItem(val type: String, val title: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    // ACHTUNG: Prüf kurz ob deine ID Long oder String ist.
    // In der DB ist es Long, im App.kt hattest du es als Long genutzt.
    // Falls deine Models String nutzen, lass String. Falls Long, ändere es hier auf Long.
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ChurchTopBar(title = "D+N App", onBackClick = null) },
        bottomBar = {
            ChurchBottomBar(
                selectedTab = BottomTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomTab.PROFILE -> onProfileClick()
                        else -> { /* other tabs not implemented yet */ }
                    }
                },
            )
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> ErrorView(msg = state.error, onRetry = { viewModel.loadAnnouncements() })
                else -> HomeContent(
                    banners = state.banners,
                    news = state.announcements,
                    onAnnouncementClick = onAnnouncementClick,
                    onViewAllClick = onViewAllClick
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    banners: List<Announcement>,
    news: List<Announcement>,
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))

        HeroBannerSection(banners = banners, onBannerClick = onAnnouncementClick)

        Spacer(modifier = Modifier.height(24.dp))
        QuickMenuSection()
        Spacer(modifier = Modifier.height(24.dp))

        LatestNewsSection(
            newsList = news,
            onItemClick = onAnnouncementClick,
            onViewAllClick = onViewAllClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}