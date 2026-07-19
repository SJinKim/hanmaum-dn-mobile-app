package com.hanmaum.dn.mobile.core.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.koin.compose.koinInject

@Composable
actual fun NotificationPermissionRequest(onResult: (Boolean) -> Unit) {
    val pushManager: PushManager = koinInject()
    LaunchedEffect(Unit) { onResult(pushManager.requestPermission()) }
}
