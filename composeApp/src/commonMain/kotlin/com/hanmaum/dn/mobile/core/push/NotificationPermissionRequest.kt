package com.hanmaum.dn.mobile.core.push

import androidx.compose.runtime.Composable

/** Fires the platform notification-permission dialog once on composition. */
@Composable
expect fun NotificationPermissionRequest(onResult: (Boolean) -> Unit)
