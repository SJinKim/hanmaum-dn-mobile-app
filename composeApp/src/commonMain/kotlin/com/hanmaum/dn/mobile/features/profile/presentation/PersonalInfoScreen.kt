package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.BirthdayField
import com.hanmaum.dn.mobile.core.presentation.dismissKeyboardOnTap
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(strings.personalInfoTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = strings.back,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    PersonalInfoContent(
                        state             = state,
                        viewModel         = viewModel,
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalInfoContent(
    state: ProfileUiState.Success,
    viewModel: ProfileViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val strings = LocalStrings.current
    val profile = state.profile

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(strings.profileSaved)
            viewModel.consumeSaveSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .dismissKeyboardOnTap()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        // Locked section — admin-managed fields, view-only.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = MaterialTheme.shapes.large,
            color    = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LockedRow(strings.labelName, "${profile.lastName} ${profile.firstName}")
                LockedRow(strings.labelEmail, profile.email ?: "—")
                LockedRow(strings.labelDivision, profile.division ?: "—")
                LockedRow(strings.labelGroup, profile.groupName ?: "—")
                LockedRow(strings.labelChurchRole, profile.churchRole ?: "—")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text  = strings.lockedFieldHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )

        Spacer(Modifier.height(24.dp))

        // Editable section — prefilled form, dirty-gated Save.
        Text(
            text     = strings.profileImageUrl,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editImageUrl,
            onValueChange = viewModel::updateImageUrl,
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
            text     = strings.labelPhone,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editPhone,
            onValueChange = viewModel::updatePhone,
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
            text     = strings.labelBirthDate,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        BirthdayField(
            value         = state.editBirthDate,
            onValueChange = viewModel::updateBirthDate,
            error         = if (!state.isBirthDateValid) strings.errorDateIncomplete else null,
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
            onValueChange = viewModel::updateStreet,
            placeholder   = { Text("Musterstraße") },
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
            text     = strings.labelHouseNumber,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        TextField(
            value         = state.editHouseNumber,
            onValueChange = viewModel::updateHouseNumber,
            placeholder   = { Text("12a") },
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
            onValueChange = viewModel::updateZipCode,
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
            onValueChange = viewModel::updateCity,
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

        Button(
            onClick  = { viewModel.saveProfile() },
            enabled  = state.isDirty && !state.isSaving && state.isBirthDateValid,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = MaterialTheme.shapes.extraSmall,
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp).semantics { contentDescription = strings.saving },
                    color       = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(strings.save, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LockedRow(label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector        = Icons.Default.Lock,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.outline,
            modifier           = Modifier.size(16.dp),
        )
    }
}
