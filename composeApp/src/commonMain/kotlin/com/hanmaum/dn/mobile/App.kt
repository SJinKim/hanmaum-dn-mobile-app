package com.hanmaum.dn.mobile

import AnnouncementDetailScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.hanmaum.dn.mobile.core.navigation.AnnouncementDetailRoute
import com.hanmaum.dn.mobile.core.navigation.HomeRoute
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
import com.hanmaum.dn.mobile.features.announcement.presentation.HomeScreen
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanmaum.dn.mobile.core.navigation.AnnouncementListRoute
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementListScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        // Controller steuert
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = LoginRoute // Startpunkt
        ) {
            // 1. LOGIN screen
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = { token ->
                        navController.navigate(HomeRoute(token)) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }

            // 2. HOME screen
            composable<HomeRoute> { backStackEntry ->
                val route: HomeRoute = backStackEntry.toRoute()

                HomeScreen(
                    token = route.token,
                    onLogout = {
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true } // Alles loeschen
                        }
                    },
                    onAnnouncementClick = { announcementId ->
                        navController.navigate(AnnouncementDetailRoute(id = announcementId, token = route.token))
                    },
                    onViewAllClick = { /* TODO: navController.navigate(AnnouncementListRoute(route.token)) */ }
                )
            }

            // 3. DETAIL screen
            composable<AnnouncementDetailRoute> { backStackEntry ->
                val route: AnnouncementDetailRoute = backStackEntry.toRoute()

                AnnouncementDetailScreen(
                    token = route.token,
                    announcementId = route.id,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 4. LIST screen
            composable<AnnouncementListRoute> { backStackEntry ->
                val route: AnnouncementListRoute = backStackEntry.toRoute()

                AnnouncementListScreen(
                    token = route.token,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { announcementId ->
                        navController.navigate(AnnouncementDetailRoute(id = announcementId, token = route.token))
                    }
                )
            }
            
        }
    }
}