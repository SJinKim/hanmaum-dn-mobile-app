package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.components.AppScreen
import com.hanmaum.dn.mobile.core.presentation.theme.AppSpacing
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofencePermissionRequest
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.notification.presentation.NotificationSettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// This is copied verbatim from ProfileScreen.kt's `ProfileViewContent` (locale row +
// language picker, theme row + theme picker, Face ID row, keep-signed-in row) for one
// commit — the profile-side copy is removed in the next task.
//
// Contains: locale row + language picker, theme row + theme picker, keep-signed-in
// row, Face ID row, and location-sharing row (restored here after being dropped in
// the profile-hub rewrite; this is its permanent home as a setting, not a profile
// field).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLocale: AppLocale,
    onLocaleChange: (AppLocale) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    keepSignedIn: Boolean,
    onKeepSignedInToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    AppScreen(
        title = strings.settingsTitle,
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.md),
        ) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier  = Modifier.fillMaxWidth(),
                onClick   = { showLanguagePicker = true },
                shape     = MaterialTheme.shapes.large,
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text  = strings.profileLanguage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = currentLocale.nativeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier  = Modifier.fillMaxWidth(),
                onClick   = { showThemePicker = true },
                shape     = MaterialTheme.shapes.large,
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text  = strings.profileTheme,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = themeModeLabel(currentTheme, strings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = MaterialTheme.shapes.large,
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = strings.profileKeepSignedIn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = strings.keepSignedInDesc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Switch(
                        checked         = keepSignedIn,
                        onCheckedChange = onKeepSignedInToggle,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = MaterialTheme.shapes.large,
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = strings.profileFaceIdLogin,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = if (biometricAvailable) strings.faceIdLoginDesc else strings.appLockUnavailable,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Switch(
                        checked         = biometricEnabled,
                        onCheckedChange = onBiometricToggle,
                        enabled         = biometricAvailable,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            LocationSharingCard()

            Spacer(Modifier.height(12.dp))
            PushNotificationCard()

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(
            currentLocale = currentLocale,
            onSelect      = { locale ->
                onLocaleChange(locale)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false },
        )
    }
    if (showThemePicker) {
        ThemePickerSheet(
            currentTheme = currentTheme,
            onSelect     = { mode ->
                onThemeChange(mode)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false },
        )
    }
}

@Composable
private fun LocationSharingCard() {
    val strings = LocalStrings.current
    val locationPreferences = koinInject<LocationPreferences>()
    val geofenceManager = koinInject<GeofenceManager>()
    val geofenceCoordinator = koinInject<GeofenceCoordinator>()
    val scope = rememberCoroutineScope()

    var enabled by remember {
        mutableStateOf(locationPreferences.isSharingEnabled() && geofenceManager.isLocationPermissionGranted())
    }
    var requestingPermission by remember { mutableStateOf(false) }

    if (requestingPermission) {
        GeofencePermissionRequest { granted ->
            requestingPermission = false
            locationPreferences.setPromptDismissed(true)
            if (granted) {
                locationPreferences.setSharingEnabled(true)
                enabled = true
                scope.launch { geofenceCoordinator.initialize() }
            } else {
                locationPreferences.setSharingEnabled(false)
                enabled = false
            }
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = strings.profileLocationSharing,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = strings.locationSharingDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Switch(
                checked         = enabled,
                onCheckedChange = { wantOn ->
                    if (wantOn) {
                        if (geofenceManager.isLocationPermissionGranted()) {
                            locationPreferences.setSharingEnabled(true)
                            enabled = true
                            scope.launch { geofenceCoordinator.initialize() }
                        } else {
                            requestingPermission = true
                        }
                    } else {
                        locationPreferences.setSharingEnabled(false)
                        enabled = false
                        scope.launch { geofenceCoordinator.stop() }
                    }
                },
            )
        }
    }
}

@Composable
private fun PushNotificationCard() {
    val strings = LocalStrings.current
    val settingsVm: NotificationSettingsViewModel = koinViewModel()
    val pushState by settingsVm.uiState.collectAsState()
    val notificationService = koinInject<NotificationService>()

    LaunchedEffect(Unit) { settingsVm.load() }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = strings.settingsPushToggle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked         = pushState.pushEnabled,
                    onCheckedChange = settingsVm::onToggle,
                )
            }
            if (!notificationService.isNotificationPermissionGranted()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = strings.settingsPushPermissionHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

private fun themeModeLabel(mode: ThemeMode, strings: AppStrings): String = when (mode) {
    ThemeMode.SYSTEM -> strings.themeSystem
    ThemeMode.LIGHT  -> strings.themeLight
    ThemeMode.DARK   -> strings.themeDark
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerSheet(
    currentTheme: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.md).padding(bottom = AppSpacing.xl)) {
            Text(
                text  = strings.selectTheme,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(mode) }
                        .padding(vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = themeModeLabel(mode, strings),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (mode == currentTheme)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                    if (mode == currentTheme) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    currentLocale: AppLocale,
    onSelect: (AppLocale) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.md).padding(bottom = AppSpacing.xl)) {
            Text(
                text  = strings.selectLanguage,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            AppLocale.entries.forEach { locale ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(locale) }
                        .padding(vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = locale.nativeName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (locale == currentLocale)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                    if (locale == currentLocale) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
