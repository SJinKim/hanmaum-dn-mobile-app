package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.hanmaum.dn.mobile.core.navigation.TopLevelDestination

@Composable
fun BottomNavBar(
    currentDestination: NavDestination?,
    onDestinationSelected: (TopLevelDestination<*>) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor   = MaterialTheme.colorScheme.primary,
            selectedTextColor   = MaterialTheme.colorScheme.primary,
            indicatorColor      = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TopLevelDestination.all.forEach { dest ->
            val selected = currentDestination?.hasRoute(dest.routeClass) == true
            NavigationBarItem(
                selected = selected,
                onClick  = { onDestinationSelected(dest) },
                icon     = { Icon(dest.icon, contentDescription = dest.label) },
                label    = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                colors   = itemColors,
            )
        }
    }
}
