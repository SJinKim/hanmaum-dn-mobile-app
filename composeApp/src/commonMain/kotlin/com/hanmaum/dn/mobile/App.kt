package com.hanmaum.dn.mobile

import AnnouncementDetailScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
// Routes (Token bitte dort entfernen!)
import com.hanmaum.dn.mobile.core.navigation.AnnouncementDetailRoute
import com.hanmaum.dn.mobile.core.navigation.HomeRoute
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
import com.hanmaum.dn.mobile.core.navigation.AnnouncementListRoute
import com.hanmaum.dn.mobile.core.navigation.RegisterRoute
import com.hanmaum.dn.mobile.core.navigation.PendingRoute
// Screens
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import com.hanmaum.dn.mobile.features.login.presentation.RegisterScreen
import com.hanmaum.dn.mobile.features.login.screen.PendingScreen
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = LoginRoute
        ) {
            // 1. LOGIN SCREEN
            composable<LoginRoute> {
                LoginScreen(
                    onNavigateToHome = {
                        // Wir übergeben keinen Token mehr! Der ist im TokenStorage.
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToPending = {
                        // Neue Route für den Warteraum
                        navController.navigate(PendingRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onRegisterClick = {
                        navController.navigate(RegisterRoute)
                    }
                )
            }

            // 2. REGISTER SCREEN
            composable<RegisterRoute> {
                RegisterScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onRegistrationSuccess = {
                        navController.popBackStack()
                        // User landet wieder im LoginScreen und kann sich einloggen (-> dann Pending)
                    }
                )
            }

            // 3. PENDING SCREEN (Warteraum)
            composable<PendingRoute> {
                PendingScreen(
                    onCheckStatusClick = {
                        // Der Button im Screen ruft ViewModel Logik auf.
                        // Wenn der Screen neu geladen werden soll, passiert das intern.
                        // Hier könnte man auch navigation logik haben, wenn nötig.
                        // Aber meistens regelt das ViewModel das neuladen.
                        // Wir geben hier einfach eine leere Lambda weiter,
                        // da das ViewModel im PendingScreen die Logik macht,
                        // ODER du übergibst das ViewModel im Screen.

                        // Einfache Lösung: PendingScreen kriegt das LoginViewModel oder ein eigenes.
                    },
                    onLogoutClick = {
                        // Zurück zum Login
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // 4. HOME SCREEN
            composable<HomeRoute> {
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
                    }
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
        }
    }
}