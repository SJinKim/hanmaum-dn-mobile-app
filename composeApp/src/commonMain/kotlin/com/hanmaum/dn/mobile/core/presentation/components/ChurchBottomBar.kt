package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.* // WICHTIG: material3 Import
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChurchBottomBar() {
    // In Material 3 nutzen wir NavigationBar statt BottomNavigation
    NavigationBar(
        containerColor = Color.White, // Hintergrund
        tonalElevation = 8.dp         // Leichter Schatten/Hervorhebung
    ) {
        // 1. Home
        NavigationBarItem(
            selected = true,
            onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("홈") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32), // Kirchen-Grün
                selectedTextColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),    // Leichter Grüner Kreis im Hintergrund
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        // 2. Sermon
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.Mic, contentDescription = "Sermon") },
            label = { Text("설교") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        // 3. Bulletin
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.Description, contentDescription = "Bulletin") },
            label = { Text("주보/소식") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        // 4. More
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
            label = { Text("더보기") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}