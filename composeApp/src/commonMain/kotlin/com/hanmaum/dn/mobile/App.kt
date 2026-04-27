package com.hanmaum.dn.mobile

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.navigation.*
import com.hanmaum.dn.mobile.core.presentation.components.BottomNavBar
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.community.presentation.CommunityStubScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListScreen
import com.hanmaum.dn.mobile.features.pending.screen.PendingScreen
import com.hanmaum.dn.mobile.features.pending.screen.SplashScreen
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        AppTheme {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = TopLevelDestination.all.any { dest ->
                currentDestination?.hasRoute(dest.routeClass) == true
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavBar(
                            currentDestination = currentDestination,
                            onDestinationSelected = { dest ->
                                navController.navigate(dest.routeInstance) {
                                    popUpTo<HomeRoute> {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = SplashRoute,
                    modifier = Modifier.padding(innerPadding).statusBarsPadding(),
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
                            onBackClick = { navController.popBackStack() },
                            onLogout = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<MinistryListRoute> {
                        MinistryListScreen(
                            onBackClick     = { navController.popBackStack() },
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
                }
            }
        }
    }
}
