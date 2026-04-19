package com.hanmaum.dn.mobile.features.login.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNavigateToPending: () -> Unit,
) {
    val viewModel: RegisterViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            onNavigateToPending()
            viewModel.onNavigationHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(56.dp))

        // Header
        Text(
            text  = "DN App",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "Create Account",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "Join our vibrant collective and start your journey.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        state.error?.let { errorMsg ->
            Text(
                text     = errorMsg,
                color    = MaterialTheme.colorScheme.error,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // Full Name (last + first stacked)
        FieldLabel("FULL NAME")
        FilledField(
            value         = state.lastName,
            onValueChange = viewModel::onLastNameChange,
            placeholder   = "성 (Last name)",
            keyboardType  = KeyboardType.Text,
        )
        Spacer(Modifier.height(8.dp))
        FilledField(
            value         = state.firstName,
            onValueChange = viewModel::onFirstNameChange,
            placeholder   = "이름 (First name)",
            keyboardType  = KeyboardType.Text,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("EMAIL ADDRESS")
        FilledField(
            value         = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder   = "hello@dnapp.com",
            keyboardType  = KeyboardType.Email,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("CITY")
        FilledField(
            value         = state.city,
            onValueChange = viewModel::onCityChange,
            placeholder   = "Your City",
            keyboardType  = KeyboardType.Text,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("POSTCODE")
        FilledField(
            value         = state.zipCode,
            onValueChange = viewModel::onZipChange,
            placeholder   = "Postcode",
            keyboardType  = KeyboardType.Number,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("PHONE NUMBER")
        FilledField(
            value         = state.phoneNumber,
            onValueChange = viewModel::onPhoneChange,
            placeholder   = "+1 (555) 000-0000",
            keyboardType  = KeyboardType.Phone,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("PASSWORD")
        FilledPasswordField(
            value         = state.password,
            onValueChange = viewModel::onPasswordChange,
        )

        // Optional section
        Spacer(Modifier.height(24.dp))
        Text(
            text  = "추가 정보 (선택사항)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(12.dp))

        FieldLabel("생일 (YYYY-MM-DD)")
        FilledField(
            value         = state.birthDate,
            onValueChange = viewModel::onBirthDateChange,
            placeholder   = "2000-01-01",
            keyboardType  = KeyboardType.Text,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("주소")
        FilledField(
            value         = state.street,
            onValueChange = viewModel::onStreetChange,
            placeholder   = "Street address",
            keyboardType  = KeyboardType.Text,
        )

        Spacer(Modifier.height(16.dp))
        FilledDropdown(
            label            = "세례 여부",
            selectedKey      = state.baptism,
            options          = mapOf(
                "INFANT"        to "유아세례 (Infant)",
                "CONFIRMATION"  to "입교 (Confirmation)",
                "BAPTISM"       to "세례 (Baptism)",
                "NONE"          to "받지 않음 (None)",
            ),
            onOptionSelected = viewModel::onBaptismChange,
        )

        Spacer(Modifier.height(16.dp))
        FilledDropdown(
            label            = "성별",
            selectedKey      = state.gender,
            options          = mapOf(
                "M" to "남성 (Male)",
                "F" to "여성 (Female)",
            ),
            onOptionSelected = viewModel::onGenderChange,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = { viewModel.register() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled  = !state.isLoading,
            shape    = MaterialTheme.shapes.extraSmall,
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier    = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    "Register",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = "Already have an account?  ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick        = onBackClick,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text  = "Login",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun FilledField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    TextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(placeholder) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        shape         = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = TextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun FilledPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value                = value,
        onValueChange        = onValueChange,
        placeholder          = { Text("Create a password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier             = Modifier.fillMaxWidth(),
        singleLine           = true,
        shape                = MaterialTheme.shapes.small,
        colors               = TextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilledDropdown(
    label: String,
    selectedKey: String,
    options: Map<String, String>,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    FieldLabel(label.uppercase())
    ExposedDropdownMenuBox(
        expanded          = expanded,
        onExpandedChange  = { expanded = !expanded },
        modifier          = Modifier.fillMaxWidth(),
    ) {
        TextField(
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            readOnly      = true,
            value         = options[selectedKey] ?: "",
            onValueChange = {},
            placeholder   = { Text("선택안함") },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape         = MaterialTheme.shapes.small,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text    = { Text("선택안함") },
                onClick = { onOptionSelected(""); expanded = false },
            )
            options.forEach { (key, displayText) ->
                DropdownMenuItem(
                    text    = { Text(displayText) },
                    onClick = { onOptionSelected(key); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

// Legacy alias kept for call-site compatibility (GenericDropdown was used in tests/previews)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericDropdown(
    label: String,
    selectedKey: String,
    options: Map<String, String>,
    onOptionSelected: (String) -> Unit,
) = FilledDropdown(label, selectedKey, options, onOptionSelected)
