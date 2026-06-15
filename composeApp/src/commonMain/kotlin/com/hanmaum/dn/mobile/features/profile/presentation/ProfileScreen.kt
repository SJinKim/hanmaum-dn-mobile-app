package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.geofence.GeofencePermissionRequest
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.AppTopBar
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    currentLocale: AppLocale,
    onLocaleChange: (AppLocale) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()
    val strings = LocalStrings.current

    // Refresh on every entry to the tab so an externally-edited profile stays
    // current without re-login. Skips while editing (guarded in the ViewModel).
    LaunchedEffect(Unit) { viewModel.loadProfile() }

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLogout()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar() },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadProfile() }) { Text(strings.retry) }
                    }
                }
                is ProfileUiState.Success -> {
                    if (state.isEditing) {
                        ProfileEditContent(
                            state            = state,
                            onPhoneChange    = { viewModel.updatePhone(it) },
                            onImageUrlChange = { viewModel.updateImageUrl(it) },
                            onStreetChange   = { viewModel.updateStreet(it) },
                            onZipCodeChange  = { viewModel.updateZipCode(it) },
                            onCityChange     = { viewModel.updateCity(it) },
                            onSave           = { viewModel.saveProfile() },
                            onCancel         = { viewModel.cancelEditing() },
                        )
                    } else {
                        ProfileViewContent(
                            profile            = state.profile,
                            currentLocale      = currentLocale,
                            currentTheme       = currentTheme,
                            biometricEnabled   = biometricEnabled,
                            biometricAvailable = biometricAvailable,
                            onEditClick        = { viewModel.startEditing() },
                            onLogoutClick      = { viewModel.logout() },
                            onLocaleChange     = onLocaleChange,
                            onThemeChange      = onThemeChange,
                            onBiometricToggle  = onBiometricToggle,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileViewContent(
    profile: MemberResponse,
    currentLocale: AppLocale,
    currentTheme: ThemeMode,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLocaleChange: (AppLocale) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        Icon(
            imageVector        = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier           = Modifier.size(100.dp),
            tint               = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))

        Text(
            "${profile.lastName} ${profile.firstName}",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
        profile.churchRole?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text  = it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick        = onEditClick,
            shape          = MaterialTheme.shapes.extraSmall,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.profileEdit, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(28.dp))

        profile.email?.let {
            InfoCard(icon = Icons.Default.Email, label = strings.labelEmail, value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.phoneNumber?.let {
            InfoCard(icon = Icons.Default.Phone, label = strings.labelPhone, value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.street?.let {
            InfoCard(icon = Icons.Default.Home, label = strings.labelStreet, value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.houseNumber?.let {
            InfoCard(icon = Icons.Default.Home, label = strings.labelHouseNumber, value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.zipCode?.let {
            InfoCard(icon = Icons.Default.Home, label = strings.labelZipCode, value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.city?.let {
            InfoCard(icon = Icons.Default.Home, label = strings.labelCity, value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.groupName?.let {
            InfoCard(icon = Icons.Default.Group, label = strings.labelPrimaryGroup, value = it)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text  = strings.profileAccountPreferences,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(12.dp))
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

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text      = """“Lead with love, serve with grace, and watch the community bloom.”""".trimIndent(),
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = MaterialTheme.colorScheme.onBackground,
                    fontStyle = FontStyle.Italic,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = "DN APP CORE VALUES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        OutlinedButton(
            onClick  = onLogoutClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = MaterialTheme.shapes.extraSmall,
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.profileLogout, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(40.dp))
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
private fun InfoCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ProfileEditContent(
    state: ProfileUiState.Success,
    onPhoneChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onZipCodeChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(strings.profileEdit, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        Text(
            text     = strings.labelPhone,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editPhone,
            onValueChange = onPhoneChange,
            placeholder   = { Text("+49-0000-0000-000") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor      = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor      = Color.Transparent,
                unfocusedIndicatorColor    = Color.Transparent,
                focusedPlaceholderColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                unfocusedPlaceholderColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text     = strings.profileImageUrl,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editImageUrl,
            onValueChange = onImageUrlChange,
            placeholder   = { Text("https://...") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor      = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor      = Color.Transparent,
                unfocusedIndicatorColor    = Color.Transparent,
                focusedPlaceholderColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                unfocusedPlaceholderColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text     = strings.labelStreet,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editStreet,
            onValueChange = onStreetChange,
            placeholder   = { Text("123 Main St") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor      = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor      = Color.Transparent,
                unfocusedIndicatorColor    = Color.Transparent,
                focusedPlaceholderColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                unfocusedPlaceholderColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text     = strings.labelZipCode,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editZipCode,
            onValueChange = onZipCodeChange,
            placeholder   = { Text("12345") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor      = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor      = Color.Transparent,
                unfocusedIndicatorColor    = Color.Transparent,
                focusedPlaceholderColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                unfocusedPlaceholderColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text     = strings.labelCity,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editCity,
            onValueChange = onCityChange,
            placeholder   = { Text("New York") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor      = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor      = Color.Transparent,
                unfocusedIndicatorColor    = Color.Transparent,
                focusedPlaceholderColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                unfocusedPlaceholderColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )

        state.saveError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = MaterialTheme.shapes.extraSmall,
            ) { Text(strings.cancel, style = MaterialTheme.typography.labelLarge) }
            Button(
                onClick  = onSave,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = MaterialTheme.shapes.extraSmall,
                enabled  = !state.isSaving,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp).semantics { contentDescription = strings.saving },
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(strings.save)
                }
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
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
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
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
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
