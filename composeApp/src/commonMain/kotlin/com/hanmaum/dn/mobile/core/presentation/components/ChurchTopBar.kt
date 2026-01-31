package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChurchTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                // --- MODUS: ZURÜCK (Detail/Register) ---
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = Color.Black
                    )
                }
            } else {
                // --- MODUS: HAUPTMENÜ (Home) ---
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "메뉴",
                        tint = Color.Black
                    )
                }
            }
        },
        actions = {
            // Zeige Profil/Alarm NUR auf der Hauptseite (wenn kein Back-Button da ist)
            // Auf der Registrierungsseite lenkt das nur ab.
            if (onBackClick == null) {
                IconButton(onClick = { /* TODO: Benachrichtigungen */ }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Alarm", tint = Color.Black)
                }
                IconButton(onClick = { /* TODO: Profil */ }) {
                    Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = Color.Black)
                }
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