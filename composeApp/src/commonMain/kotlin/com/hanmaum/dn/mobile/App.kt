package com.hanmaum.dn.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import androidx.compose.foundation.isSystemInDarkTheme
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.security.rememberBiometricAuthenticator
import com.hanmaum.dn.mobile.features.profile.presentation.SettingsScreen
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.domain.repository.LocaleRepository
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.DeStrings
import com.hanmaum.dn.mobile.core.i18n.EnStrings
import com.hanmaum.dn.mobile.core.i18n.KoStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.navigation.*
import com.hanmaum.dn.mobile.core.notification.NotificationDestination
import com.hanmaum.dn.mobile.core.notification.NotificationRouter
import com.hanmaum.dn.mobile.core.presentation.components.BottomNavBar
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpHost
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
import com.hanmaum.dn.mobile.features.notification.presentation.NotificationListScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.album.presentation.AlbumDetailScreen
import com.hanmaum.dn.mobile.features.album.presentation.PhotoViewerScreen
import com.hanmaum.dn.mobile.features.album.presentation.albums.AlbumsScreen
import com.hanmaum.dn.mobile.features.calendar.presentation.CalendarScreen
import com.hanmaum.dn.mobile.features.community.presentation.CommunityStubScreen
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceScreen
import com.hanmaum.dn.mobile.features.floorplan.presentation.FloorPlanScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.NurtureDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.list.ParticipationScreen
import com.hanmaum.dn.mobile.features.pending.screen.PendingScreen
import com.hanmaum.dn.mobile.features.pending.screen.SplashScreen
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileScreen
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinContext {
        val localeRepo = koinInject<LocaleRepository>()
        var locale by remember { mutableStateOf(localeRepo.getLocale()) }

        val themeRepo = koinInject<ThemeRepository>()
        var themeMode by remember { mutableStateOf(themeRepo.getThemeMode()) }
        val darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val strings = remember(locale) {
            when (locale) {
                AppLocale.EN -> EnStrings
                AppLocale.KO -> KoStrings
                AppLocale.DE -> DeStrings
            }
        }

        CompositionLocalProvider(LocalStrings provides strings) {
            AppTheme(darkTheme = darkTheme) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = TopLevelDestination.all.any { dest ->
                currentDestination?.hasRoute(dest.routeClass) == true
            }

            // A notification tap has to be handled here rather than inside a
            // screen: it arrives whatever the member was last looking at, and a
            // collector inside HomeScreen only runs while Home is composed.
            val notificationRouter = koinInject<NotificationRouter>()
            LaunchedEffect(navController) {
                notificationRouter.pending.collect { destination ->
                    when (destination) {
                        null -> Unit
                        NotificationDestination.Attendance -> {
                            notificationRouter.consume()
                            navController.navigate(AttendanceRoute) { launchSingleTop = true }
                        }
                    }
                }
            }

            // The dock floats over the content instead of taking a strip at the
            // bottom, so screens scroll underneath it and fade out through the
            // scroll edge. That means a Box, not a Scaffold bottomBar.
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = SplashRoute,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable<SplashRoute> {
                        SplashScreen(
                            onNavigate = { route ->
                                val targetRoute: Any = when (route) {
                                    NavRoute.Home            -> HomeRoute
                                    NavRoute.Login           -> LoginRoute
                                    NavRoute.PendingApproval -> PendingRoute
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo<SplashRoute> { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<LoginRoute> {
                        LoginScreen(
                            onNavigateToHome = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                }
                            },
                            onNavigateToPending = {
                                navController.navigate(PendingRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                }
                            },
                            onRegisterClick = { navController.navigate(RegisterRoute) },
                        )
                    }

                    composable<RegisterRoute> {
                        RegisterScreen(
                            onBackClick = { navController.popBackStack() },
                            onNavigateToPending = {
                                navController.navigate(PendingRoute) {
                                    popUpTo<LoginRoute> { inclusive = false }
                                    popUpTo<RegisterRoute> { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<PendingRoute> {
                        PendingScreen(
                            onNavigateToHome = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<PendingRoute> { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<HomeRoute> {
                        HomeScreen(
                            onAnnouncementClick = { id ->
                                navController.navigate(AnnouncementDetailRoute(id = id))
                            },
                            onViewAllClick = { navController.navigate(AnnouncementListRoute) },
                            onFloorPlanClick = { navController.navigate(FloorPlanRoute) },
                            onProfileClick = { navController.navigate(ProfileRoute) },
                            onNotificationsClick = { navController.navigate(NotificationsRoute) },
                            onNurtureClick = {
                                navController.navigate(ParticipationRoute(ParticipationRoute.TAB_NURTURE))
                            },
                            onServeClick = {
                                navController.navigate(ParticipationRoute(ParticipationRoute.TAB_SERVE))
                            },
                            onAttendanceClick = { navController.navigate(AttendanceRoute) },
                            onCommunityClick = { navController.navigate(CommunityRoute) },
                        )
                    }

                    composable<SettingsRoute> {
                        val authPrefs = koinInject<AuthPreferences>()
                        val locationPrefs = koinInject<LocationPreferences>()
                        val geofence = koinInject<GeofenceCoordinator>()
                        val credentials = koinInject<CredentialStore>()
                        val scope = rememberCoroutineScope()
                        val biometrics = rememberBiometricAuthenticator()
                        var keepSignedIn by remember { mutableStateOf(authPrefs.isKeepSignedInEnabled()) }
                        var biometricEnabled by remember { mutableStateOf(authPrefs.isBiometricEnabled()) }
                        var locationEnabled by remember { mutableStateOf(locationPrefs.isSharingEnabled()) }

                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            currentTheme = themeMode,
                            onThemeChange = { mode ->
                                themeRepo.setThemeMode(mode)
                                themeMode = mode
                            },
                            currentLocale = locale,
                            onLocaleChange = { newLocale ->
                                localeRepo.setLocale(newLocale)
                                locale = newLocale
                            },
                            keepSignedIn = keepSignedIn,
                            onKeepSignedInChange = { value ->
                                authPrefs.setKeepSignedInEnabled(value)
                                keepSignedIn = value
                                // turning it off also clears biometrics — see AuthPreferencesImpl
                                biometricEnabled = authPrefs.isBiometricEnabled()
                                // nothing may stay behind that could sign someone in
                                if (!value) credentials.clear()
                            },
                            biometricEnabled = biometricEnabled,
                            biometricAvailable = biometrics.isAvailable(),
                            onBiometricChange = { value ->
                                authPrefs.setBiometricEnabled(value)
                                biometricEnabled = value
                                if (!value) credentials.clear()
                            },
                            locationEnabled = locationEnabled,
                            onLocationChange = { value ->
                                locationPrefs.setSharingEnabled(value)
                                locationPrefs.setPromptDismissed(true)
                                locationEnabled = value
                                // Register or tear down the OS geofence right away
                                // rather than waiting for the next cold start.
                                if (value) scope.launch { geofence.initialize() } else scope.launch { geofence.stop() }
                            },
                        )
                    }

                    composable<NotificationsRoute> {
                        // main's list screen, not the v2 redesign: it carries
                        // swipe-to-delete and clear-all, which the redesign
                        // never had. Restyling it is tracked separately.
                        NotificationListScreen(
                            onBack = { navController.popBackStack() },
                            onOpenAnnouncement = { id -> navController.navigate(AnnouncementDetailRoute(id)) },
                        )
                    }

                    composable<AnnouncementDetailRoute> { backStackEntry ->
                        val route: AnnouncementDetailRoute = backStackEntry.toRoute()
                        AnnouncementDetailScreen(
                            announcementId = route.id,
                            onBackClick    = { navController.popBackStack() },
                        )
                    }

                    composable<AnnouncementListRoute> {
                        AnnouncementListScreen(
                            onBackClick = { navController.popBackStack() },
                            onItemClick = { id ->
                                navController.navigate(AnnouncementDetailRoute(id = id))
                            },
                        )
                    }

                    composable<ProfileRoute> {
                        ProfileScreen(
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() },
                            onSettings = { navController.navigate(SettingsRoute) },
                        )
                    }

                    composable<ParticipationRoute> { backStackEntry ->
                        val route: ParticipationRoute = backStackEntry.toRoute()
                        ParticipationScreen(
                            initialTab = route.tab,
                            onBackClick = { navController.popBackStack() },
                            onMinistryClick = { publicId ->
                                navController.navigate(MinistryDetailRoute(publicId = publicId))
                            },
                            onNurtureClick = { publicId ->
                                navController.navigate(NurtureDetailRoute(publicId = publicId))
                            },
                        )
                    }

                    composable<MinistryDetailRoute> { backStackEntry ->
                        val route: MinistryDetailRoute = backStackEntry.toRoute()
                        MinistryDetailScreen(
                            publicId    = route.publicId,
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    composable<NurtureDetailRoute> { backStackEntry ->
                        val route: NurtureDetailRoute = backStackEntry.toRoute()
                        NurtureDetailScreen(
                            publicId = route.publicId,
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    composable<CommunityRoute> {
                        CommunityStubScreen(onBackClick = { navController.popBackStack() })
                    }

                    composable<FloorPlanRoute> {
                        FloorPlanScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    composable<AttendanceRoute> {
                        AttendanceScreen(onBackClick = { navController.popBackStack() })
                    }

                    composable<AlbumsRoute> {
                        AlbumsScreen(
                            onBackClick = { navController.popBackStack() },
                            onAlbumClick = { pcloudCode, albumName ->
                                navController.navigate(AlbumDetailRoute(pcloudCode = pcloudCode, albumName = albumName))
                            }
                        )
                    }

                    composable<AlbumDetailRoute> { backStackEntry ->
                        val route: AlbumDetailRoute = backStackEntry.toRoute()
                        AlbumDetailScreen(
                            pcloudCode = route.pcloudCode,
                            albumName = route.albumName,
                            onPhotoClick = { url -> navController.navigate(PhotoViewerRoute(photoUrl = url)) },
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    composable<PhotoViewerRoute> { backStackEntry ->
                        val route: PhotoViewerRoute = backStackEntry.toRoute()
                        PhotoViewerScreen(photoUrl = route.photoUrl, onBackClick = { navController.popBackStack() })
                    }

                    composable<CalendarRoute> { CalendarScreen(onBackClick = { navController.popBackStack() }) }
                }

                if (showBottomBar) {
                    BottomNavBar(
                        currentDestination = currentDestination,
                        onDestinationSelected = { dest ->
                            navController.navigate(dest.routeInstance) {
                                popUpTo<HomeRoute> { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    // Sits over the whole graph, not inside a screen: the sheet
                    // has to be able to appear whatever the member is looking at.
                    EventRsvpHost()
                }
            }
            }
        } // CompositionLocalProvider
    }
}
