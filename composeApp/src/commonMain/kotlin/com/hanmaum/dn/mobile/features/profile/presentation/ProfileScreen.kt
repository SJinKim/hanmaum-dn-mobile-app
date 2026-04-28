package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import com.hanmaum.dn.mobile.core.presentation.components.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.theme.SoftPeach
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()

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
                        Button(onClick = { viewModel.loadProfile() }) { Text("다시 시도") }
                    }
                }
                is ProfileUiState.Success -> {
                    if (state.isEditing) {
                        ProfileEditContent(
                            state          = state,
                            onPhoneChange  = { viewModel.updatePhone(it) },
                            onImageUrlChange = { viewModel.updateImageUrl(it) },
                            onSave         = { viewModel.saveProfile() },
                            onCancel       = { viewModel.cancelEditing() },
                        )
                    } else {
                        ProfileViewContent(
                            profile       = state.profile,
                            onEditClick   = { viewModel.startEditing() },
                            onLogoutClick = { viewModel.logout() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileViewContent(
    profile: MemberResponse,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // Avatar
        Icon(
            imageVector        = Icons.Default.AccountCircle,
            contentDescription = "프로필 아이콘",
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
            Text("Edit Profile", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(28.dp))

        // Info cards
        profile.email?.let {
            InfoCard(icon = Icons.Default.Email, label = "EMAIL ADDRESS", value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.phoneNumber?.let {
            InfoCard(icon = Icons.Default.Phone, label = "PHONE NUMBER", value = it)
            Spacer(Modifier.height(12.dp))
        }
        profile.groupName?.let {
            InfoCard(icon = Icons.Default.Group, label = "PRIMARY GROUP", value = it)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Account Preferences section
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text  = "Account Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = MaterialTheme.shapes.large,
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text  = "LANGUAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "English (US)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Quote section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftPeach, MaterialTheme.shapes.large)
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text      = "\u201CLead with love, serve with grace, and watch the community bloom.\u201D",
                    style     = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
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
            Text("Logout", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(40.dp))
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
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("프로필 수정", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        Text(
            text     = "PHONE NUMBER",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editPhone,
            onValueChange = onPhoneChange,
            placeholder   = { Text("+1 (555) 000-0000") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text     = "PROFILE IMAGE URL",
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
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
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
            ) { Text("취소", style = MaterialTheme.typography.labelLarge) }
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
                        modifier    = Modifier.size(18.dp).semantics { contentDescription = "저장 중" },
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("저장")
                }
            }
        }
    }
}
