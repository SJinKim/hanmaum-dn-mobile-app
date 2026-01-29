package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChurchTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "D+N App", // Hanmaum DN App
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO: Drawer öffnen */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Benachrichtigungen */ }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Alarm", tint = Color.Black)
            }
            IconButton(onClick = { /* TODO: Profil */ }) {
                Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = Color.Black)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color.Black,
            actionIconContentColor = Color.Black,
            navigationIconContentColor = Color.Black
        )
    )
}