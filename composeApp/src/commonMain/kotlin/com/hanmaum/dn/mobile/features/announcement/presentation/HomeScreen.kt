package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.compose.animation.core.spring
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
import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.geofence.GeofencePermissionRequest
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.core.push.PushPreferences
import com.hanmaum.dn.mobile.features.announcement.presentation.components.BibleVerseSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.HeroBannerSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.LatestNewsSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.QuickAccessSection
import com.hanmaum.dn.mobile.features.announcement.presentation.components.MorningServiceCard
import com.hanmaum.dn.mobile.features.announcement.presentation.components.WeeklyVerseSection
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceUiState
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceViewModel
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onFloorPlanClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val attendanceViewModel: AttendanceViewModel = koinViewModel()
    val attendanceState by attendanceViewModel.uiState.collectAsStateWithLifecycle()

    val geofenceCoordinator: GeofenceCoordinator = koinInject()
    val geofenceManager: GeofenceManager = koinInject()
    val locationPreferences: LocationPreferences = koinInject()
    val pushPreferences: PushPreferences = koinInject()
    val notificationService: NotificationService = koinInject()

    var showRationale by remember { mutableStateOf(false) }
    var requestingPermission by remember { mutableStateOf(false) }
    var showPushPriming by remember { mutableStateOf(false) }

    // Refresh on every entry to the tab so web-app changes appear without re-login.
    LaunchedEffect(Unit) { viewModel.loadAnnouncements() }

    LaunchedEffect(Unit) {
        geofenceCoordinator.initialize()
        // Ask for location only once; respect a prior decision (see Profile to change it).
        // Push priming is deferred to a later entry when the location card is already
        // showing, so the two rationale cards never stack on top of each other.
        if (!locationPreferences.isPromptDismissed() && !geofenceManager.isLocationPermissionGranted()) {
            showRationale = true
        } else if (!pushPreferences.isPromptDismissed() && !notificationService.isNotificationPermissionGranted()) {
            showPushPriming = true
        }
    }

    val coroutineScope = rememberCoroutineScope()

    if (requestingPermission) {
        GeofencePermissionRequest { granted ->
            requestingPermission = false
            showRationale = false
            locationPreferences.setPromptDismissed(true)
            if (granted) {
                locationPreferences.setSharingEnabled(true)
                coroutineScope.launch { geofenceCoordinator.initialize() }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                unseenCount = state.unseenCount,
                onNotificationsClick = onNotificationsClick,
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
                    onFloorPlanClick    = onFloorPlanClick,
                )
            }
            if (showRationale) {
                GeofenceRationaleCard(
                    onAllow = { requestingPermission = true },
                    onDismiss = {
                        locationPreferences.setPromptDismissed(true)
                        showRationale = false
                    },
                )
            }
            if (showPushPriming) {
                PushPrimingCard(
                    onEnable = {
                        // wired to PushManager in the push-plumbing task
                        pushPreferences.setPromptDismissed(true)
                        showPushPriming = false
                    },
                    onDismiss = {
                        pushPreferences.setPromptDismissed(true)
                        showPushPriming = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(unseenCount: Int, onNotificationsClick: () -> Unit) {
    val strings = LocalStrings.current
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
        BadgedBox(
            badge = {
                // DESIGN.md: badge appears/disappears with a spring scale, never a bare pop.
                androidx.compose.animation.AnimatedVisibility(
                    visible = unseenCount > 0,
                    enter = androidx.compose.animation.scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f)),
                    exit = androidx.compose.animation.scaleOut(spring(dampingRatio = 0.6f, stiffness = 400f)),
                ) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        Text(if (unseenCount > 9) "9+" else unseenCount.toString())
                    }
                }
            },
        ) {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = strings.notifications,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
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
    onFloorPlanClick: () -> Unit,
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

        Spacer(modifier = Modifier.height(24.dp))

        QuickAccessSection(
            onFloorPlanClick = onFloorPlanClick,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GeofenceRationaleCard(onAllow: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
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
                OutlinedButton(onClick = onDismiss) { Text(strings.laterButton) }
                Button(onClick = onAllow) { Text(strings.allowPermission) }
            }
        }
    }
}

@Composable
private fun PushPrimingCard(onEnable: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.pushPrimingTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.pushPrimingBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) { Text(strings.laterButton) }
                Button(onClick = onEnable) { Text(strings.pushPrimingEnable) }
            }
        }
    }
}
