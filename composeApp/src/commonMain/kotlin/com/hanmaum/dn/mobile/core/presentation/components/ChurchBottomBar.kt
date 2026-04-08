package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class BottomTab { HOME, SERMON, QT, PROFILE }

@Composable
fun ChurchBottomBar(
    selectedTab: BottomTab = BottomTab.HOME,
    onTabSelected: (BottomTab) -> Unit = {},
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

        NavigationBarItem(
            selected = selectedTab == BottomTab.HOME,
            onClick  = { onTabSelected(BottomTab.HOME) },
            icon     = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label    = { Text("홈", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.SERMON,
            onClick  = { onTabSelected(BottomTab.SERMON) },
            icon     = { Icon(Icons.Default.Mic, contentDescription = "순소식") },
            label    = { Text("순소식", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.QT,
            onClick  = { onTabSelected(BottomTab.QT) },
            icon     = { Icon(Icons.Default.Description, contentDescription = "QT") },
            label    = { Text("QT", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.PROFILE,
            onClick  = { onTabSelected(BottomTab.PROFILE) },
            icon     = { Icon(Icons.Default.Person, contentDescription = "프로필") },
            label    = { Text("프로필", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
    }
}
