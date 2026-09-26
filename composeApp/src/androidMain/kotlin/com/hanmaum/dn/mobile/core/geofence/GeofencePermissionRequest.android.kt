package com.hanmaum.dn.mobile.core.geofence

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * The first dialog batch for [sdk]. From API 30 on, background location must NOT be in it:
 * a request that mixes foreground and background location is ignored as a whole, so the
 * notification permission riding along in the same batch was never asked either (#103).
 * API 29 is the one level that still accepts background location in the same batch.
 */
internal fun foregroundPermissions(sdk: Int): List<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (sdk == Build.VERSION_CODES.Q) add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    // The attendance notification is the geofence's only purpose, so ask for it here.
    if (sdk >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

/** API 30+ asks for background location on its own, once foreground location is granted. */
internal fun needsSeparateBackgroundRequest(sdk: Int): Boolean = sdk >= Build.VERSION_CODES.R

@Composable
actual fun GeofencePermissionRequest(onResult: (Boolean) -> Unit) {
    // The geofence still works while the app is open without "Allow all the time", so the
    // result reports foreground location only, whatever the user picks here.
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onResult(true) }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (locationGranted && needsSeparateBackgroundRequest(Build.VERSION.SDK_INT)) {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            onResult(locationGranted)
        }
    }

    LaunchedEffect(Unit) {
        foregroundLauncher.launch(foregroundPermissions(Build.VERSION.SDK_INT).toTypedArray())
    }
}
