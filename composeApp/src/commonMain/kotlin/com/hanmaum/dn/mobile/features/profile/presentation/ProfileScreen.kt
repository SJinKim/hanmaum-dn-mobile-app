package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.AppTopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenPersonalInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()
    val strings = LocalStrings.current

    // Refresh on every entry to the tab so an externally-edited profile stays
    // current without re-login.
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
                    ProfileHubContent(
                        state              = state,
                        onOpenPersonalInfo = onOpenPersonalInfo,
                        onOpenSettings     = onOpenSettings,
                        onLogoutClick      = { viewModel.logout() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHubContent(
    state: ProfileUiState.Success,
    onOpenPersonalInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val profile = state.profile

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
        profile.division?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        profile.groupName?.let {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    it,
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        HubMenuRow(
            icon    = Icons.Default.Person,
            label   = strings.personalInfoTitle,
            onClick = onOpenPersonalInfo,
        )
        Spacer(Modifier.height(12.dp))
        HubMenuRow(
            icon    = Icons.Default.Settings,
            label   = strings.settingsTitle,
            onClick = onOpenSettings,
        )

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

        Spacer(Modifier.height(80.dp)) // pill-nav clearance (space_bottom_nav)
    }
}

@Composable
private fun HubMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label         = "hubMenuRowScale",
    )

    Surface(
        onClick           = onClick,
        interactionSource = interaction,
        shape             = MaterialTheme.shapes.small,
        color             = MaterialTheme.colorScheme.surfaceVariant,
        modifier          = Modifier
            .fillMaxWidth()
            .scale(scale),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text     = label,
                style    = MaterialTheme.typography.titleMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
