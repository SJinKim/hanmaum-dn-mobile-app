package com.hanmaum.dn.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
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
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.LocaleRepository
import com.hanmaum.dn.mobile.core.domain.repository.ThemeRepository
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.DeStrings
import com.hanmaum.dn.mobile.core.i18n.EnStrings
import com.hanmaum.dn.mobile.core.i18n.KoStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.navigation.*
import com.hanmaum.dn.mobile.core.presentation.components.FloatingPillNav
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.album.presentation.AlbumDetailScreen
import com.hanmaum.dn.mobile.features.album.presentation.PhotoViewerScreen
import com.hanmaum.dn.mobile.features.album.presentation.albums.AlbumsScreen
import com.hanmaum.dn.mobile.features.calendar.presentation.CalendarScreen
import com.hanmaum.dn.mobile.features.community.presentation.CommunityStubScreen
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceScreen
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpHost
import com.hanmaum.dn.mobile.features.floorplan.presentation.FloorPlanScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListScreen
import com.hanmaum.dn.mobile.features.notification.presentation.NotificationListScreen
import com.hanmaum.dn.mobile.features.pending.screen.PendingScreen
import com.hanmaum.dn.mobile.features.pending.screen.SplashScreen
import com.hanmaum.dn.mobile.features.profile.presentation.PersonalInfoScreen
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileScreen
import com.hanmaum.dn.mobile.features.profile.presentation.SettingsScreen
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.security.rememberBiometricAuthenticator

@Composable
fun App() {
    KoinContext {
        val localeRepo = koinInject<LocaleRepository>()
        var locale by remember { mutableStateOf(localeRepo.getLocale()) }
        val themeRepo = koinInject<ThemeRepository>()
        var themeMode by remember { mutableStateOf(themeRepo.getThemeMode()) }

        // ── Face ID sign-in ───────────────────────────────────────────────────
        val tokenStorage = koinInject<TokenStorage>()
        val credentialStore = koinInject<CredentialStore>()
        val biometric = rememberBiometricAuthenticator()
        var biometricEnabled by remember { mutableStateOf(tokenStorage.isBiometricEnabled()) }
        var keepSignedIn by remember { mutableStateOf(tokenStorage.isKeepSignedIn()) }
        val strings = remember(locale) {
            when (locale) {
                AppLocale.EN -> EnStrings
                AppLocale.KO -> KoStrings
                AppLocale.DE -> DeStrings
            }
        }

        CompositionLocalProvider(LocalStrings provides strings) {
            AppTheme(themeMode = themeMode) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = TopLevelDestination.all.any { dest ->
                currentDestination?.hasRoute(dest.routeClass) == true
            }

            Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding(),
                ) {
                NavHost(
                    navController = navController,
                    startDestination = SplashRoute,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (showBottomBar) 80.dp else 0.dp),
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
                            onNotificationsClick = { navController.navigate(NotificationListRoute) },
                            onOpenAnnouncementDeepLink = { id ->
                                navController.navigate(AnnouncementDetailRoute(id = id))
                            },
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

                    composable<NotificationListRoute> {
                        NotificationListScreen(
                            onBack = { navController.popBackStack() },
                            onOpenAnnouncement = { id -> navController.navigate(AnnouncementDetailRoute(id)) },
                        )
                    }

                    composable<ProfileRoute> {
                        ProfileScreen(
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onOpenPersonalInfo = { navController.navigate(PersonalInfoRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                        )
                    }

                    composable<PersonalInfoRoute> {
                        PersonalInfoScreen(onBack = { navController.popBackStack() })
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            currentLocale = locale,
                            onLocaleChange = { newLocale ->
                                localeRepo.setLocale(newLocale)
                                locale = newLocale
                            },
                            currentTheme = themeMode,
                            onThemeChange = { newMode ->
                                themeRepo.setThemeMode(newMode)
                                themeMode = newMode
                            },
                            biometricEnabled = biometricEnabled,
                            biometricAvailable = biometric.isAvailable(),
                            onBiometricToggle = { enable ->
                                if (enable) {
                                    tokenStorage.setBiometricEnabled(true)
                                    biometricEnabled = true
                                } else {
                                    tokenStorage.setBiometricEnabled(false)
                                    credentialStore.clear()
                                    biometricEnabled = false
                                }
                            },
                            keepSignedIn = keepSignedIn,
                            onKeepSignedInToggle = { enable ->
                                tokenStorage.setKeepSignedIn(enable)
                                keepSignedIn = enable
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<MinistryListRoute> {
                        MinistryListScreen(
                            onMinistryClick = { publicId ->
                                navController.navigate(MinistryDetailRoute(publicId = publicId))
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

                    composable<CommunityRoute> {
                        CommunityStubScreen()
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

                    composable<CalendarRoute> { CalendarScreen() }
                }

                if (showBottomBar) {
                    FloatingPillNav(
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
                    EventRsvpHost()
                }
            }
            }

            } // Box(fillMaxSize)
            }
        } // CompositionLocalProvider
    }
}
