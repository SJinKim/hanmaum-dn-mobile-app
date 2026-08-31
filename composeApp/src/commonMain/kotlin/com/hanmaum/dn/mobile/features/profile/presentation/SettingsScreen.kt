package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode
import com.hanmaum.dn.mobile.core.geofence.GeofencePermissionRequest
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.notification.presentation.NotificationSettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Everything the member decides for themselves, in one place.
 *
 * The three groups answer three different questions — how the app looks, how
 * signing in works, and what the app is allowed to know — so they are kept
 * visually separate rather than run together as one long list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    currentLocale: AppLocale,
    onLocaleChange: (AppLocale) -> Unit,
    keepSignedIn: Boolean,
    onKeepSignedInChange: (Boolean) -> Unit,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onBiometricChange: (Boolean) -> Unit,
    locationEnabled: Boolean,
    onLocationChange: (Boolean) -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current

    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    // Composing the request is what launches it, so it is gated on a flag.
    var requestingLocation by remember { mutableStateOf(false) }
    // After the first refusal Android stops showing the dialog at all, so a
    // second tap looks like a dead switch unless we say what happened.
    var locationRefused by remember { mutableStateOf(false) }

    val themeLabel = when (currentTheme) {
        ThemeMode.SYSTEM -> strings.themeSystem
        ThemeMode.LIGHT -> strings.themeLight
        ThemeMode.DARK -> strings.themeDark
    }

    if (requestingLocation) {
        GeofencePermissionRequest { granted ->
            requestingLocation = false
            locationRefused = !granted
            // Only the system's answer decides — a switch that flips without the
            // permission would promise monitoring that can never run.
            onLocationChange(granted)
        }
    }

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.settingsTitle, onBack = onBack)

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                SettingsGroup(strings.settingsGroupDisplay) {
                    ValueRow(strings.profileTheme, themeLabel) { showThemeSheet = true }
                    RowDivider()
                    ValueRow(strings.profileLanguage, currentLocale.nativeName) { showLanguageSheet = true }
                }

                SettingsGroup(strings.settingsGroupSignIn) {
                    SwitchRow(
                        label = strings.profileKeepSignedIn,
                        description = strings.keepSignedInDesc,
                        checked = keepSignedIn,
                        onChange = onKeepSignedInChange,
                    )
                    RowDivider()
                    SwitchRow(
                        label = strings.profileFaceIdLogin,
                        // Unlocking a session only means something when one is kept.
                        description = when {
                            !biometricAvailable -> strings.appLockUnavailable
                            else -> strings.faceIdLoginDesc
                        },
                        checked = biometricEnabled && keepSignedIn,
                        enabled = biometricAvailable && keepSignedIn,
                        onChange = onBiometricChange,
                    )
                }

                SettingsGroup(strings.settingsGroupNotifications) {
                    PushSwitchRow()
                }

                SettingsGroup(strings.settingsGroupPrivacy) {
                    SwitchRow(
                        label = strings.profileLocationSharing,
                        description = if (locationRefused && !locationEnabled) {
                            strings.settingsLocationDenied
                        } else {
                            strings.locationSharingDesc
                        },
                        checked = locationEnabled,
                        onChange = { want ->
                            if (want) {
                                locationRefused = false
                                requestingLocation = true
                            } else {
                                onLocationChange(false)
                            }
                        },
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }, containerColor = c.surface) {
            SheetOptions(
                title = strings.profileTheme,
                subtitle = strings.settingsThemeSubtitle,
                options = listOf(
                    ThemeMode.SYSTEM to strings.themeSystem,
                    ThemeMode.LIGHT to strings.themeLight,
                    ThemeMode.DARK to strings.themeDark,
                ).map { (mode, label) ->
                    SheetOption(label, null, mode == currentTheme) {
                        onThemeChange(mode)
                        showThemeSheet = false
                    }
                },
            )
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }, containerColor = c.surface) {
            SheetOptions(
                title = strings.profileLanguage,
                subtitle = null,
                options = AppLocale.entries.map { locale ->
                    SheetOption(locale.nativeName, locale.name, locale == currentLocale) {
                        onLocaleChange(locale)
                        showLanguageSheet = false
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val c = DnTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = DnTheme.typography.label, color = c.textTertiary)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape),
        ) { content() }
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DnTheme.colors.strokeSubtle))
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    val c = DnTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = DnTheme.typography.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = DnTheme.typography.caption, color = c.textSecondary)
        Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                label,
                style = DnTheme.typography.bodyStrong,
                color = if (enabled) c.textPrimary else c.textTertiary,
            )
            Text(description, style = DnTheme.typography.caption, color = c.textSecondary)
        }
        DnSwitch(checked = checked, enabled = enabled, onChange = { onChange(!checked) })
    }
}

/** Spring-animated so the knob settles rather than snapping. */
@Composable
private fun DnSwitch(checked: Boolean, enabled: Boolean, onChange: () -> Unit) {
    val c = DnTheme.colors
    val track by animateColorAsState(
        when {
            !enabled -> c.surface3
            checked -> c.lime
            else -> c.surface3
        },
        label = "track",
    )
    val knobOffset by animateDpAsState(
        if (checked) 18.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "knob",
    )
    Box(
        Modifier
            .width(46.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(enabled = enabled, onClick = onChange)
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked && enabled) c.onLime else c.textTertiary),
        )
    }
}

/**
 * The push opt-out, carried over from the pre-redesign settings screen.
 *
 * Its own composable because it owns a ViewModel the rest of the screen has no
 * use for. When the OS permission is missing the toggle stays visible but the
 * description says why flipping it will not help — hiding the row instead
 * would leave a member who denied the prompt with no explanation.
 */
@Composable
private fun PushSwitchRow() {
    val strings = LocalStrings.current
    val settingsVm: NotificationSettingsViewModel = koinViewModel()
    val pushState by settingsVm.uiState.collectAsState()
    val notificationService = koinInject<NotificationService>()
    val permitted = remember { notificationService.isNotificationPermissionGranted() }

    LaunchedEffect(Unit) { settingsVm.load() }

    SwitchRow(
        label = strings.settingsPushToggle,
        description = if (permitted) strings.settingsPushDesc else strings.settingsPushPermissionHint,
        checked = pushState.pushEnabled,
        onChange = settingsVm::onToggle,
        enabled = permitted,
    )
}
