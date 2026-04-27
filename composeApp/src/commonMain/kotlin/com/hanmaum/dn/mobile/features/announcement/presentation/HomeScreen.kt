package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofencePermissionRequest
import com.hanmaum.dn.mobile.core.presentation.components.BottomTab
import com.hanmaum.dn.mobile.core.presentation.components.ChurchBottomBar
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.announcement.presentation.components.BibleVerseSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.HeroBannerSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.LatestNewsSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.MorningServiceCard
import com.hanmaum.dn.mobile.features.announcement.presentation.components.WeeklyVerseSection
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceUiState
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceViewModel
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import kotlinx.coroutines.launch

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

    val attendanceViewModel: AttendanceViewModel = koinViewModel()
    val attendanceState by attendanceViewModel.uiState.collectAsStateWithLifecycle()

    val geofenceCoordinator: GeofenceCoordinator = koinInject()
    val geofenceManager: GeofenceManager = koinInject()

    var showRationale by remember { mutableStateOf(false) }
    var requestingPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        geofenceCoordinator.initialize()
        if (!geofenceManager.isLocationPermissionGranted()) {
            showRationale = true
        }
    }

    val coroutineScope = rememberCoroutineScope()

    if (requestingPermission) {
        GeofencePermissionRequest { granted ->
            requestingPermission = false
            showRationale = false
            if (granted) {
                coroutineScope.launch { geofenceCoordinator.initialize() }
            }
        }
    }

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
                    state           = state,
                    attendanceState = attendanceState,
                    onAnnouncementClick = onAnnouncementClick,
                    onViewAllClick      = onViewAllClick,
                    onCheckIn           = attendanceViewModel::checkIn,
                )
            }
            if (showRationale) {
                GeofenceRationaleCard(
                    onAllow = { requestingPermission = true },
                    onDismiss = { showRationale = false },
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
    attendanceState: AttendanceUiState,
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onCheckIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HeroBannerSection(
            banners     = state.banners,
            onBannerClick = onAnnouncementClick,
            isLoading   = state.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))
        MorningServiceCard(
            state     = attendanceState,
            onCheckIn = onCheckIn,
        )
        Spacer(modifier = Modifier.height(8.dp))

        BibleVerseSection(onViewAllClick = onViewAllClick)

        Spacer(modifier = Modifier.height(24.dp))

        WeeklyVerseSection()

        Spacer(modifier = Modifier.height(24.dp))

        LatestNewsSection(
            newsList    = state.announcements,
            onItemClick = onAnnouncementClick,
            onViewAllClick = onViewAllClick,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GeofenceRationaleCard(onAllow: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "도착 알림 설정",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "예배 시간에 교회 근처에 오시면 출석 알림을 보내드립니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) { Text("나중에") }
                Button(onClick = onAllow) { Text("권한 허용") }
            }
        }
    }
}
