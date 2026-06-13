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
import com.hanmaum.dn.mobile.features.floorplan.presentation.FloorPlanScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListScreen
import com.hanmaum.dn.mobile.features.pending.screen.PendingScreen
import com.hanmaum.dn.mobile.features.pending.screen.SplashScreen
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileScreen
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.session.SessionManager
import com.hanmaum.dn.mobile.core.session.SessionValidator
import com.hanmaum.dn.mobile.core.presentation.components.LockScreen
import com.hanmaum.dn.mobile.core.security.BiometricResult
import com.hanmaum.dn.mobile.core.security.RefreshTokenVault
import com.hanmaum.dn.mobile.core.security.UnlockResult
import com.hanmaum.dn.mobile.core.security.rememberBiometricAuthenticator
import com.hanmaum.dn.mobile.core.security.rememberRefreshTokenUnlocker
import kotlinx.coroutines.launch

@Composable
fun App() {
    KoinContext {
        val localeRepo = koinInject<LocaleRepository>()
        var locale by remember { mutableStateOf(localeRepo.getLocale()) }
        val themeRepo = koinInject<ThemeRepository>()
        var themeMode by remember { mutableStateOf(themeRepo.getThemeMode()) }

        // ── Biometric app lock ────────────────────────────────────────────────
        val tokenStorage = koinInject<TokenStorage>()
        val sessionManager = koinInject<SessionManager>()
        val sessionValidator = koinInject<SessionValidator>()
        val refreshVault = koinInject<RefreshTokenVault>()
        val unlocker = rememberRefreshTokenUnlocker()
        // biometric + biometricEnabled stay for the Profile toggle UI.
        val biometric = rememberBiometricAuthenticator()
        val lockScope = rememberCoroutineScope()
        var biometricEnabled by remember { mutableStateOf(tokenStorage.isBiometricEnabled()) }
        // Locked whenever a stay-signed-in session has a gated token that isn't
        // yet unlocked into memory. (Biometric opt-in no longer gates this — the
        // token is always behind device auth.)
        var locked by remember {
            mutableStateOf(refreshVault.hasStored() && refreshVault.current() == null)
        }
        // Re-lock on background: drop the in-memory token and re-prompt next foreground.
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            if (refreshVault.hasStored()) {
                refreshVault.lock()
                locked = true
            }
        }
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

            // Single global logout sink. Every logout path funnels through
            // SessionManager and emits here; this is the only place that
            // navigates to Login on sign-out, so there's no double-navigation.
            LaunchedEffect(Unit) {
                sessionManager.events.collect {
                    locked = false
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            // Auto-prompt the gated read whenever locked.
            LaunchedEffect(locked) {
                if (locked) {
                    when (val result = unlocker.unlock(strings.lockSubtitle)) {
                        is UnlockResult.Success -> {
                            refreshVault.acceptUnlocked(result.token)
                            locked = false
                            sessionValidator.isSessionValid() // dead token → logout sink → Login
                        }
                        UnlockResult.Empty -> {
                            // Nothing to unlock → go to login.
                            locked = false
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }; launchSingleTop = true
                            }
                        }
                        UnlockResult.Cancelled, UnlockResult.Failed -> {
                            // Stay locked; the LockScreen's retry button re-triggers.
                        }
                    }
                }
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
                                    lockScope.launch {
                                        val result = biometric.authenticate(
                                            strings.lockTitle, strings.lockSubtitle, strings.lockUsePassword,
                                        )
                                        if (result == BiometricResult.SUCCESS) {
                                            tokenStorage.setBiometricEnabled(true)
                                            biometricEnabled = true
                                        }
                                    }
                                } else {
                                    tokenStorage.setBiometricEnabled(false)
                                    biometricEnabled = false
                                }
                            },
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
                }
            }
            }

            if (locked) {
                LockScreen(
                    onUnlock = {
                        lockScope.launch {
                            when (val result = unlocker.unlock(strings.lockSubtitle)) {
                                is UnlockResult.Success -> {
                                    refreshVault.acceptUnlocked(result.token)
                                    locked = false
                                    sessionValidator.isSessionValid()
                                }
                                UnlockResult.Empty -> {
                                    locked = false
                                    navController.navigate(LoginRoute) {
                                        popUpTo(0) { inclusive = true }; launchSingleTop = true
                                    }
                                }
                                UnlockResult.Cancelled, UnlockResult.Failed -> Unit
                            }
                        }
                    },
                    onUsePassword = {
                        // Deliberate sign-out → canonical pipeline; sink navigates to Login.
                        lockScope.launch { sessionManager.logout() }
                    },
                )
            }
            } // lock overlay Box(fillMaxSize)
            }
        } // CompositionLocalProvider
    }
}
