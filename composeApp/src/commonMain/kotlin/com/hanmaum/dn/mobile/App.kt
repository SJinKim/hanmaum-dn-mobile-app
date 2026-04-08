package com.hanmaum.dn.mobile

import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementDetailScreen
import androidx.compose.runtime.*
import com.hanmaum.dn.mobile.core.presentation.theme.AppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.core.navigation.*
// Screens
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.pending.screen.PendingScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen
import com.hanmaum.dn.mobile.features.pending.screen.SplashScreen
import com.hanmaum.dn.mobile.core.navigation.ProfileRoute
import com.hanmaum.dn.mobile.features.profile.presentation.ProfileScreen
import com.hanmaum.dn.mobile.core.navigation.MinistryDetailRoute
import com.hanmaum.dn.mobile.core.navigation.MinistryListRoute
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailScreen
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        AppTheme {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = SplashRoute
            ) {
                // 1. SPLASH (Auto Login Logik)
                composable<SplashRoute>  {
                    SplashScreen(
                        onNavigate = { route ->
                            val targetRoute: Any = when(route) {
                                NavRoute.Home -> HomeRoute
                                NavRoute.Login -> LoginRoute
                                NavRoute.PendingApproval -> PendingRoute
                            }

                            navController.navigate(targetRoute) {
                                popUpTo<SplashRoute> { inclusive = true }
                            }
                        }
                    )
                }

                // 2. LOGIN SCREEN
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
                        onRegisterClick = {
                            navController.navigate(RegisterRoute)
                        }
                    )
                }

                // 3. REGISTER SCREEN
                composable<RegisterRoute> {
                    RegisterScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToPending = {
                            navController.navigate(PendingRoute) {
                                popUpTo<LoginRoute> { inclusive = false }
                                popUpTo<RegisterRoute> { inclusive = true }
                            }
                        }
                    )
                }

                // 4. PENDING SCREEN (Warteraum)
                composable<PendingRoute> {
                    PendingScreen(
                        onNavigateToHome = {
                            navController.navigate(HomeRoute) {
                                popUpTo<PendingRoute> { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate(LoginRoute) {
                                // Alles löschen
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // 4. HOME SCREEN
                composable<HomeRoute>  {
                    HomeScreen(

                        onLogout = {
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onAnnouncementClick = { announcementId ->
                            navController.navigate(AnnouncementDetailRoute(id = announcementId))
                        },
                        onViewAllClick = {
                            navController.navigate(AnnouncementListRoute)
                        },
                        onProfileClick = {
                            navController.navigate(ProfileRoute)
                        },
                        onMinistryClick = {
                            navController.navigate(MinistryListRoute)
                        },
                    )
                }

                // 5. DETAIL SCREEN
                composable<AnnouncementDetailRoute> { backStackEntry ->
                    val route: AnnouncementDetailRoute = backStackEntry.toRoute()

                    AnnouncementDetailScreen(
                        announcementId = route.id,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // 6. LIST SCREEN
                composable<AnnouncementListRoute> {
                    AnnouncementListScreen(
                        onBackClick = { navController.popBackStack() },
                        onItemClick = { announcementId ->
                            navController.navigate(AnnouncementDetailRoute(id = announcementId))
                        }
                    )
                }

                // 7. PROFILE SCREEN
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

                // 8. MINISTRY LIST
                composable<MinistryListRoute> {
                    MinistryListScreen(
                        onBackClick = { navController.popBackStack() },
                        onMinistryClick = { publicId ->
                            navController.navigate(MinistryDetailRoute(publicId = publicId))
                        },
                    )
                }

                // 9. MINISTRY DETAIL
                composable<MinistryDetailRoute> { backStackEntry ->
                    val route: MinistryDetailRoute = backStackEntry.toRoute()
                    MinistryDetailScreen(
                        publicId = route.publicId,
                        onBackClick = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}