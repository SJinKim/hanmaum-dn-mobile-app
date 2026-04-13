package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.BottomTab
import com.hanmaum.dn.mobile.core.presentation.components.ChurchBottomBar
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.announcement.presentation.components.BibleVerseSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.HeroBannerSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.LatestNewsSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.WeeklyVerseSection
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMinistryClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onNewsClick: () -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar() },
        bottomBar = {
            ChurchBottomBar(
                selectedTab = BottomTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomTab.PROFILE    -> onProfileClick()
                        BottomTab.MINISTRIES -> onMinistryClick()
                        BottomTab.COMMUNITY  -> onCommunityClick()
                        BottomTab.NEWS       -> onNewsClick()
                        else                 -> {}
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                state.error != null -> ErrorView(
                    msg = state.error,
                    onRetry = { viewModel.loadAnnouncements() },
                )
                else -> HomeContent(
                    state = state,
                    onAnnouncementClick = onAnnouncementClick,
                    onViewAllClick = onViewAllClick,
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "DN App",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = { /* 알림 기능 추가 예정 */ }) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "알림",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HeroBannerSection(
            banners = state.banners,
            onBannerClick = onAnnouncementClick,
            isLoading = state.isLoading,
        )

        Spacer(modifier = Modifier.height(8.dp))

        BibleVerseSection(onViewAllClick = onViewAllClick)

        Spacer(modifier = Modifier.height(24.dp))

        WeeklyVerseSection()

        Spacer(modifier = Modifier.height(24.dp))

        LatestNewsSection(
            newsList = state.announcements,
            onItemClick = onAnnouncementClick,
            onViewAllClick = onViewAllClick,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
