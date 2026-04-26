package com.hanmaum.dn.mobile.core.geofence

import androidx.compose.runtime.Composable

@Composable
expect fun GeofencePermissionRequest(onResult: (Boolean) -> Unit)
