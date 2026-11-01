package com.hanmaum.dn.mobile

import com.hanmaum.dn.mobile.core.presentation.LanguageSelectionScreen
import com.hanmaum.dn.mobile.core.presentation.LanguageViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavBackStackEntry
import com.hanmaum.dn.mobile.features.consent.presentation.ConsentViewModel
import com.hanmaum.dn.mobile.features.consent.presentation.ConsentPhase
import com.hanmaum.dn.mobile.features.consent.presentation.ConsentGate
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import androidx.compose.foundation.isSystemInDarkTheme
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
import com.hanmaum.dn.mobile.core.domain.repository.AuthPreferences
import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.features.profile.presentation.SettingsScreen
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.core.security.BiometricVault
import com.hanmaum.dn.mobile.core.security.InstallationGuard
import com.hanmaum.dn.mobile.core.i18n.appStringsFor
import com.hanmaum.dn.mobile.core.i18n.ProvideAppLanguage
import com.hanmaum.dn.mobile.core.navigation.*
import com.hanmaum.dn.mobile.core.notification.NotificationDestination
import com.hanmaum.dn.mobile.core.notification.NotificationRouter
import com.hanmaum.dn.mobile.core.presentation.components.BottomNavBar
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpHost
import com.hanmaum.dn.mobile.features.pending.screen.RejectedScreen
import com.hanmaum.dn.mobile.core.navigation.RejectedRoute
import com.hanmaum.dn.mobile.features.events.presentation.RsvpScreen
import com.hanmaum.dn.mobile.core.navigation.RsvpRoute
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
import com.hanmaum.dn.mobile.features.notification.presentation.NotificationListScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.bulletin.presentation.BulletinScreen
import com.hanmaum.dn.mobile.core.navigation.BulletinRoute
import com.hanmaum.dn.mobile.features.album.presentation.AlbumDetailScreen
import com.hanmaum.dn.mobile.features.album.presentation.PhotoViewerScreen
import com.hanmaum.dn.mobile.features.album.presentation.albums.AlbumsScreen
import com.hanmaum.dn.mobile.features.calendar.presentation.CalendarScreen
import com.hanmaum.dn.mobile.features.community.presentation.CommunityStubScreen
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceScreen
import com.hanmaum.dn.mobile.features.attendance.presentation.history.AttendanceHistoryScreen
import com.hanmaum.dn.mobile.features.floorplan.presentation.FloorPlanScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.list.ParticipationScreen
import com.hanmaum.dn.mobile.features.training.presentation.detail.NurtureDetailScreen
import com.hanmaum.dn.mobile.features.training.presentation.myapplications.MyApplicationsScreen
import com.hanmaum.dn.mobile.features.pending.screen.PendingScreen
import com.hanmaum.dn.mobile.features.pending.screen.SplashScreen
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileScreen
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinContext {
        // First thing, before any screen composes and before anything reads a
        // token: a fresh installation must not inherit what the Keychain kept
        // from the app it replaced (#225).
        val installationGuard = koinInject<InstallationGuard>()
        remember { installationGuard.enforce() }

        val languageViewModel = koinViewModel<LanguageViewModel>()
        val languageState by languageViewModel.uiState.collectAsState()
        val locale = languageState.locale

        val themeRepo = koinInject<ThemeRepository>()
        var themeMode by remember { mutableStateOf(themeRepo.getThemeMode()) }
        val darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val strings = remember(locale) { appStringsFor(locale) }

        ProvideAppLanguage(strings) {
            AppTheme(darkTheme = darkTheme) {
            if (languageState.needsSelection) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    LanguageSelectionScreen(languageViewModel::select)
                }
                return@AppTheme
            }
            val consentViewModel = koinViewModel<ConsentViewModel>()
            val consentState by consentViewModel.uiState.collectAsState()
            val navController = rememberNavController()
            val onConsentSignedOut: () -> Unit = {
                navController.navigate(LoginRoute()) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Signed-out destinations are the one place the consent state is dropped, so every
            // way out of a session (profile logout, lock-screen logout, refusal) is covered.
            LaunchedEffect(currentDestination) {
                val signedOutScreen = currentDestination?.let {
                    it.hasRoute(SplashRoute::class) || it.hasRoute(LoginRoute::class) ||
                        it.hasRoute(RejectedRoute::class)
                } == true
                if (signedOutScreen) consentViewModel.reset()
            }

            // The geofence follows the consent state; no screen starts or stops it on its own.
            val geofence = koinInject<GeofenceCoordinator>()
            val locationAllowed by consentViewModel.locationAllowed.collectAsState()
            LaunchedEffect(locationAllowed) {
                if (locationAllowed) geofence.initialize() else geofence.stop()
            }

            val showBottomBar = consentState.phase == ConsentPhase.READY &&
                consentViewModel.hasCurrentSession() && TopLevelDestination.all.any { dest ->
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
                        NotificationDestination.Rsvp -> {
                            notificationRouter.consume()
                            navController.navigate(RsvpRoute) { launchSingleTop = true }
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
                                    NavRoute.Login           -> LoginRoute()
                                    NavRoute.PendingApproval -> PendingRoute
                                    NavRoute.Rejected        -> RejectedRoute
                                }
                                navController.navigate(targetRoute) {
                                    // Belt and braces: the splash reports its
                                    // destination once and clears it, but a second
                                    // report would stack a second copy of the same
                                    // screen — two login screens, one behind the
                                    // other (#213).
                                    launchSingleTop = true
                                    if (route == NavRoute.Rejected) {
                                        popUpTo(0) { inclusive = true }
                                    } else {
                                        popUpTo<SplashRoute> { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable<LoginRoute> { backStackEntry ->
                        LoginScreen(
                            notice = backStackEntry.toRoute<LoginRoute>().notice,
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
                            onNavigateToRejected = {
                                // popUpTo(0): the whole back stack goes, so there is
                                // nothing behind this screen to return to.
                                navController.navigate(RejectedRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onRegisterClick = { navController.navigate(RegisterRoute) },
                        )
                    }

                    composable<RegisterRoute> {
                        RegisterScreen(
                            onBackClick = { navController.popBackStack() },
                            onRegistered = { destination, notice ->
                                val target = when (destination) {
                                    NavRoute.Home -> HomeRoute
                                    NavRoute.Rejected -> RejectedRoute
                                    NavRoute.Login -> LoginRoute(notice)
                                    NavRoute.PendingApproval -> PendingRoute
                                }
                                navController.navigate(target) {
                                    // Registration is done either way, so the
                                    // form does not stay on the stack for a
                                    // back gesture to return to and resubmit.
                                    popUpTo<RegisterRoute> { inclusive = true }
                                }
                            },
                        )
                    }

                    // No back handler and no way out: the screen is the end of the
                    // road for a refused member, by design.
                    composable<RejectedRoute> { RejectedScreen() }

                    consentProtectedComposable<PendingRoute>(consentViewModel, onConsentSignedOut, activeDestination = false) {
                        PendingScreen(
                            onNavigateToHome = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<PendingRoute> { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute()) {
                                    launchSingleTop = true
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToRejected = {
                                // popUpTo(0): the whole back stack goes, so there is
                                // nothing behind this screen to return to.
                                navController.navigate(RejectedRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    consentProtectedComposable<HomeRoute>(consentViewModel, onConsentSignedOut) {
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
                            onBulletinClick = { navController.navigate(BulletinRoute) },
                        )
                    }

                    consentProtectedComposable<BulletinRoute>(consentViewModel, onConsentSignedOut) {
                        BulletinScreen(onBack = { navController.popBackStack() })
                    }

                    consentProtectedComposable<SettingsRoute>(consentViewModel, onConsentSignedOut) {
                        val authPrefs = koinInject<AuthPreferences>()
                        val locationPrefs = koinInject<LocationPreferences>()
                        val vault = koinInject<BiometricVault>()
                        val scope = rememberCoroutineScope()
                        var keepSignedIn by remember { mutableStateOf(authPrefs.isKeepSignedInEnabled()) }
                        var locationEnabled by remember { mutableStateOf(locationPrefs.isSharingEnabled()) }

                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            consentViewModel = consentViewModel,
                            currentTheme = themeMode,
                            onThemeChange = { mode ->
                                themeRepo.setThemeMode(mode)
                                themeMode = mode
                            },
                            currentLocale = locale,
                            onLocaleChange = languageViewModel::select,
                            keepSignedIn = keepSignedIn,
                            onKeepSignedInChange = { value ->
                                authPrefs.setKeepSignedInEnabled(value)
                                keepSignedIn = value
                                // Turning it off also clears the biometric flag (see
                                // AuthPreferencesImpl); the Face ID row re-reads it.
                                // Nothing may stay behind that could sign someone in.
                                if (!value) vault.clear()
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

                    consentProtectedComposable<NotificationsRoute>(consentViewModel, onConsentSignedOut) {
                        // main's list screen, not the v2 redesign: it carries
                        // swipe-to-delete and clear-all, which the redesign
                        // never had. Restyling it is tracked separately.
                        NotificationListScreen(
                            onBack = { navController.popBackStack() },
                            onOpenAnnouncement = { id -> navController.navigate(AnnouncementDetailRoute(id)) },
                        )
                    }

                    consentProtectedComposable<AnnouncementDetailRoute>(consentViewModel, onConsentSignedOut) { backStackEntry ->
                        val route: AnnouncementDetailRoute = backStackEntry.toRoute()
                        AnnouncementDetailScreen(
                            announcementId = route.id,
                            onBackClick    = { navController.popBackStack() },
                        )
                    }

                    consentProtectedComposable<AnnouncementListRoute>(consentViewModel, onConsentSignedOut) {
                        AnnouncementListScreen(
                            onBackClick = { navController.popBackStack() },
                            onItemClick = { id ->
                                navController.navigate(AnnouncementDetailRoute(id = id))
                            },
                        )
                    }

                    consentProtectedComposable<ProfileRoute>(consentViewModel, onConsentSignedOut) {
                        ProfileScreen(
                            onLogout = {
                                navController.navigate(LoginRoute()) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() },
                            onSettings = { navController.navigate(SettingsRoute) },
                            onMyApplications = { navController.navigate(MyApplicationsRoute) },
                        )
                    }

                    consentProtectedComposable<ParticipationRoute>(consentViewModel, onConsentSignedOut) { backStackEntry ->
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

                    consentProtectedComposable<MinistryDetailRoute>(consentViewModel, onConsentSignedOut) { backStackEntry ->
                        val route: MinistryDetailRoute = backStackEntry.toRoute()
                        MinistryDetailScreen(
                            publicId    = route.publicId,
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    consentProtectedComposable<NurtureDetailRoute>(consentViewModel, onConsentSignedOut) { backStackEntry ->
                        val route: NurtureDetailRoute = backStackEntry.toRoute()
                        NurtureDetailScreen(
                            publicId = route.publicId,
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    consentProtectedComposable<MyApplicationsRoute>(consentViewModel, onConsentSignedOut) {
                        MyApplicationsScreen(
                            onBackClick = { navController.popBackStack() },
                            onApplicationClick = { publicId ->
                                navController.navigate(NurtureDetailRoute(publicId = publicId))
                            },
                        )
                    }

                    consentProtectedComposable<CommunityRoute>(consentViewModel, onConsentSignedOut) {
                        CommunityStubScreen(onBackClick = { navController.popBackStack() })
                    }

                    consentProtectedComposable<FloorPlanRoute>(consentViewModel, onConsentSignedOut) {
                        FloorPlanScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    consentProtectedComposable<AttendanceRoute>(consentViewModel, onConsentSignedOut) {
                        AttendanceScreen(
                            onBackClick = { navController.popBackStack() },
                            onRsvpClick = { navController.navigate(RsvpRoute) },
                            onHistoryClick = { navController.navigate(AttendanceHistoryRoute) },
                            // popUpTo(0): a PENDING member has no Home to go back to.
                            onNavigateToPending = {
                                navController.navigate(PendingRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                    consentProtectedComposable<AttendanceHistoryRoute>(consentViewModel, onConsentSignedOut) {
                        AttendanceHistoryScreen(onBackClick = { navController.popBackStack() })
                    }

                    consentProtectedComposable<RsvpRoute>(consentViewModel, onConsentSignedOut) {
                        RsvpScreen(onBackClick = { navController.popBackStack() })
                    }

                    consentProtectedComposable<AlbumsRoute>(consentViewModel, onConsentSignedOut) {
                        AlbumsScreen(
                            onBackClick = { navController.popBackStack() },
                            onAlbumClick = { pcloudCode, albumName ->
                                navController.navigate(AlbumDetailRoute(pcloudCode = pcloudCode, albumName = albumName))
                            }
                        )
                    }

                    consentProtectedComposable<AlbumDetailRoute>(consentViewModel, onConsentSignedOut) { backStackEntry ->
                        val route: AlbumDetailRoute = backStackEntry.toRoute()
                        AlbumDetailScreen(
                            pcloudCode = route.pcloudCode,
                            albumName = route.albumName,
                            onPhotoClick = { url -> navController.navigate(PhotoViewerRoute(photoUrl = url)) },
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    consentProtectedComposable<PhotoViewerRoute>(consentViewModel, onConsentSignedOut) { backStackEntry ->
                        val route: PhotoViewerRoute = backStackEntry.toRoute()
                        PhotoViewerScreen(photoUrl = route.photoUrl, onBackClick = { navController.popBackStack() })
                    }

                    consentProtectedComposable<CalendarRoute>(consentViewModel, onConsentSignedOut) { CalendarScreen(onBackClick = { navController.popBackStack() }) }
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

/** Every member-content destination uses the session gate, including restored routes and push targets. */
private inline fun <reified T : Any> NavGraphBuilder.consentProtectedComposable(
    viewModel: ConsentViewModel,
    noinline onSignedOut: () -> Unit,
    activeDestination: Boolean = true,
    crossinline content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        ConsentGate(viewModel, activeDestination, onSignedOut) { content(entry) }
    }
}
