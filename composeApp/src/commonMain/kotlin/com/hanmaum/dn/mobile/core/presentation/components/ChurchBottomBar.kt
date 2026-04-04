package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class BottomTab { HOME, SERMON, QT, PROFILE }

@Composable
fun ChurchBottomBar(
    selectedTab: BottomTab = BottomTab.HOME,
    onTabSelected: (BottomTab) -> Unit = {},
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
    ) {
        NavigationBarItem(
            selected = selectedTab == BottomTab.HOME,
            onClick = { onTabSelected(BottomTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label = { Text("홈") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                selectedTextColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
            ),
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.SERMON,
            onClick = { onTabSelected(BottomTab.SERMON) },
            icon = { Icon(Icons.Default.Mic, contentDescription = "순소식") },
            label = { Text("순소식") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
            ),
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.QT,
            onClick = { onTabSelected(BottomTab.QT) },
            icon = { Icon(Icons.Default.Description, contentDescription = "QT") },
            label = { Text("QT") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
            ),
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.PROFILE,
            onClick = { onTabSelected(BottomTab.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "프로필") },
            label = { Text("프로필") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                selectedTextColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
            ),
        )
    }
}
