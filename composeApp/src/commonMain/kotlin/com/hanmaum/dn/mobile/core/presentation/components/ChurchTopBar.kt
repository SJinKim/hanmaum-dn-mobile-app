package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChurchTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector        = Icons.Default.Menu,
                        contentDescription = "메뉴",
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            if (onBackClick == null) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector        = Icons.Outlined.Notifications,
                        contentDescription = "알림",
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector        = Icons.Outlined.Person,
                        contentDescription = "프로필",
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor             = MaterialTheme.colorScheme.surface,
            titleContentColor          = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor     = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
