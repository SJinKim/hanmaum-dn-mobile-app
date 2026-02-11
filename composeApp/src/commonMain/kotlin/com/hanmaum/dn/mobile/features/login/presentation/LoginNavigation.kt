package com.hanmaum.dn.mobile.features.login.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.hanmaum.dn.mobile.core.navigation.HomeRoute
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
import com.hanmaum.dn.mobile.core.navigation.RegisterRoute
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen

fun NavGraphBuilder.loginScreen(navController: NavController) {
    composable<LoginRoute> {
        LoginScreen(
            // ACTIVE -> Home
            onNavigateToHome = {
                navController.navigate(HomeRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                }
            },
            // PENDING -> Warteraum
            onNavigateToPending = {
                navController.navigate(RegisterRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                }
            },
            // Noch kein Account
            onRegisterClick = {
                navController.navigate(RegisterRoute)
            }
        )
    }
}