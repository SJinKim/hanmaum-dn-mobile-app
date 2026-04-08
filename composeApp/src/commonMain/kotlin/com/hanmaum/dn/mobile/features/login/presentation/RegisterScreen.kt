package com.hanmaum.dn.mobile.features.login.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.ChurchTopBar
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNavigateToPending: () -> Unit
) {
    val viewModel: RegisterViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            onNavigateToPending()
            viewModel.onNavigationHandled()
        }
    }

    Scaffold(
        topBar = {
            ChurchTopBar(title = "한마음 새가족 등록", onBackClick = onBackClick)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text("Pflichtfelder sind mit * markiert", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // --- PFLICHTFELDER ---
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::onLastNameChange,
                    label = { Text("성 / LastName *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    label = { Text("이름 / FirstName *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("이메일 / Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("비밀번호 / Password *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    // Versteckt die Zeichen (Punkte statt Text)
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                OutlinedTextField(
                    value = state.zipCode,
                    onValueChange = viewModel::onZipChange,
                    label = { Text("우편번호 / Zip Code *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                OutlinedTextField(
                    value = state.city,
                    onValueChange = viewModel::onCityChange,
                    label = { Text("도시 / City *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Zusätzliche Infos (Optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                // --- OPTIONALE FELDER ---

                // Dropdown für Taufe (kein Stern mehr *)
                GenericDropdown(
                    label = "세례 여부 / Taufe",
                    selectedKey = state.baptism,
                    options = mapOf(
                        "INFANT" to "유아세례 (Infant)",
                        "CONFIRMATION" to "입교 (Confirmation)",
                        "BAPTISM" to "세례 (Baptism)",
                        "NONE" to "받지 않음 (None)"
                    ),
                    onOptionSelected = viewModel::onBaptismChange
                )

                // Dropdown für Gender
                GenericDropdown(
                    label = "성별 / Geschlecht",
                    selectedKey = state.gender,
                    options = mapOf(
                        "M" to "남성 / Male",
                        "F" to "여성 / Female"
                    ),
                    onOptionSelected = viewModel::onGenderChange
                )

                OutlinedTextField(
                    value = state.birthDate,
                    onValueChange = viewModel::onBirthDateChange,
                    label = { Text("생일 / Birthdate (YYYY-MM-DD)") },
                    placeholder = { Text("2000-01-01") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("전화번호 / Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                OutlinedTextField(
                    value = state.street,
                    onValueChange = viewModel::onStreetChange,
                    label = { Text("주소 / Street") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape  = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick  = { viewModel.register() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled  = !state.isLoading,
                    shape    = MaterialTheme.shapes.extraSmall,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color       = MaterialTheme.colorScheme.onPrimary,
                            modifier    = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("등록하기", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Generische Dropdown Komponente (Wiederverwendbar für Taufe & Gender)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericDropdown(
    label: String,
    selectedKey: String,
    options: Map<String, String>, // Key -> Display Text
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            readOnly      = true,
            value         = options[selectedKey] ?: "",
            onValueChange = {},
            label         = { Text(label) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape         = MaterialTheme.shapes.small,
            colors        = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Option "Leer/Zurücksetzen" hinzufügen
            DropdownMenuItem(
                text = { Text("선택안함 / None") },
                onClick = {
                    onOptionSelected("")
                    expanded = false
                }
            )
            options.forEach { (key, displayText) ->
                DropdownMenuItem(
                    text = { Text(displayText) },
                    onClick = {
                        onOptionSelected(key)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}