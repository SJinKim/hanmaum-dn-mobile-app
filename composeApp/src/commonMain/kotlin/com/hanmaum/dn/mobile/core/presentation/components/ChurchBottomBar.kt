package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class BottomTab { HOME, COMMUNITY, MINISTRIES, NEWS, PROFILE }

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
            selected = selectedTab == BottomTab.COMMUNITY,
            onClick  = { onTabSelected(BottomTab.COMMUNITY) },
            icon     = { Icon(Icons.Default.Group, contentDescription = "커뮤니티") },
            label    = { Text("커뮤니티", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.MINISTRIES,
            onClick  = { onTabSelected(BottomTab.MINISTRIES) },
            icon     = { Icon(Icons.Default.Star, contentDescription = "사역") },
            label    = { Text("사역", style = MaterialTheme.typography.labelSmall) },
            colors   = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.NEWS,
            onClick  = { onTabSelected(BottomTab.NEWS) },
            icon     = { Icon(Icons.Default.Newspaper, contentDescription = "소식") },
            label    = { Text("소식", style = MaterialTheme.typography.labelSmall) },
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
