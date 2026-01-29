package com.hanmaum.dn.mobile.features.login.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.hanmaum.dn.mobile.core.navigation.HomeRoute
import com.hanmaum.dn.mobile.core.navigation.LoginRoute
import com.hanmaum.dn.mobile.features.login.screen.LoginScreen

fun NavGraphBuilder.loginScreen(navController: NavController) {
    composable<LoginRoute> {
        LoginScreen(
            onLoginSuccess = { token ->
                navController.navigate(HomeRoute(token)) {
                    popUpTo(LoginRoute) { inclusive = true }
                }
            }
        )
    }
}