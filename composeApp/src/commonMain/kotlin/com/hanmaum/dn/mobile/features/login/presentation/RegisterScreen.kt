package com.hanmaum.dn.mobile.features.login.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.features.login.domain.model.Countries
import com.hanmaum.dn.mobile.features.login.domain.model.Country
import com.hanmaum.dn.mobile.features.login.domain.model.PasswordCriteria
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNavigateToPending: () -> Unit,
) {
    val viewModel: RegisterViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    // One FocusRequester per validated field so we can jump to the first invalid one.
    val focusRequesters = remember {
        RegisterField.entries.associateWith { FocusRequester() }
    }

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            onNavigateToPending()
            viewModel.onNavigationHandled()
        }
    }

    // Pre-select the dial-code country from the device locale on first composition.
    LaunchedEffect(Unit) {
        viewModel.setDefaultPhoneCountry(Locale.current.region)
    }

    // After a failed submit, focus + scroll to the first invalid field.
    LaunchedEffect(state.focusTarget) {
        state.focusTarget?.let { target ->
            runCatching { focusRequesters[target]?.requestFocus() }
            viewModel.onFocusHandled()
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

        state.bannerError?.let { banner ->
            val isPositive = banner is RegisterBanner.RegisteredPleaseLogin
            Text(
                text     = when (banner) {
                    is RegisterBanner.ServerMessage         -> banner.text
                    RegisterBanner.Generic                  -> strings.registerFailed
                    RegisterBanner.RegisteredPleaseLogin    -> strings.registerSuccessLogin
                },
                color    = if (isPositive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // Full Name (last + first stacked)
        FieldLabel("FULL NAME")
        FilledField(
            value          = state.lastName,
            onValueChange  = viewModel::onLastNameChange,
            placeholder    = "성 (Last name)",
            keyboardType   = KeyboardType.Text,
            error          = strings.messageFor(state.lastNameError),
            focusRequester = focusRequesters[RegisterField.LAST_NAME],
        )
        Spacer(Modifier.height(8.dp))
        FilledField(
            value          = state.firstName,
            onValueChange  = viewModel::onFirstNameChange,
            placeholder    = "이름 (First name)",
            keyboardType   = KeyboardType.Text,
            error          = strings.messageFor(state.firstNameError),
            focusRequester = focusRequesters[RegisterField.FIRST_NAME],
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("EMAIL ADDRESS")
        FilledField(
            value          = state.email,
            onValueChange  = viewModel::onEmailChange,
            placeholder    = "hello@dnapp.com",
            keyboardType   = KeyboardType.Email,
            error          = strings.messageFor(state.emailError),
            focusRequester = focusRequesters[RegisterField.EMAIL],
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("CITY")
        FilledField(
            value          = state.city,
            onValueChange  = viewModel::onCityChange,
            placeholder    = "Your City",
            keyboardType   = KeyboardType.Text,
            error          = strings.messageFor(state.cityError),
            focusRequester = focusRequesters[RegisterField.CITY],
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("POSTCODE")
        FilledField(
            value          = state.zipCode,
            onValueChange  = viewModel::onZipChange,
            placeholder    = "Postcode",
            keyboardType   = KeyboardType.Number,
            error          = strings.messageFor(state.zipCodeError),
            focusRequester = focusRequesters[RegisterField.ZIP_CODE],
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("PHONE NUMBER")
        PhoneNumberField(
            countryIso        = state.phoneCountryIso,
            number            = state.phoneNumber,
            onNumberChange    = viewModel::onPhoneChange,
            onCountrySelected = viewModel::onPhoneCountryChange,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("PASSWORD")
        FilledPasswordField(
            value          = state.password,
            onValueChange  = viewModel::onPasswordChange,
            criteria       = state.passwordCriteria,
            error          = strings.messageFor(state.passwordError),
            focusRequester = focusRequesters[RegisterField.PASSWORD],
        )

        // Optional section
        Spacer(Modifier.height(24.dp))
        Text(
            text  = "추가 정보 (선택사항)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(12.dp))

        FieldLabel("생일 (YYYY.MM.DD)")
        BirthdayField(
            value         = state.birthDate,
            onValueChange = viewModel::onBirthDateChange,
            error         = strings.messageFor(state.birthDateError),
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("주소")
        // German address split: street name (70%) + house number (30%) on one row.
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top,
        ) {
            Box(Modifier.weight(0.7f)) {
                FilledField(
                    value          = state.street,
                    onValueChange  = viewModel::onStreetChange,
                    placeholder    = "Straße",
                    keyboardType   = KeyboardType.Text,
                    error          = strings.messageFor(state.streetError),
                    focusRequester = focusRequesters[RegisterField.STREET],
                )
            }
            Box(Modifier.weight(0.3f)) {
                FilledField(
                    value          = state.houseNumber,
                    onValueChange  = viewModel::onHouseNumberChange,
                    placeholder    = "Nr.",
                    keyboardType   = KeyboardType.Text,
                    error          = strings.messageFor(state.houseNumberError),
                    focusRequester = focusRequesters[RegisterField.HOUSE_NUMBER],
                )
            }
        }

        // Baptism — 4 options → bottom sheet selector
        Spacer(Modifier.height(16.dp))
        SheetOptionSelector(
            label            = "세례 여부",
            selectedKey      = state.baptism,
            options          = mapOf(
                "INFANT"       to "유아세례 (Infant)",
                "CONFIRMATION" to "입교 (Confirmation)",
                "BAPTISM"      to "세례 (Baptism)",
                "NONE"         to "받지 않음 (None)",
            ),
            onOptionSelected = viewModel::onBaptismChange,
        )

        // Gender — 2 options → inline option buttons
        Spacer(Modifier.height(16.dp))
        InlineOptionSelector(
            label            = "성별",
            selectedKey      = state.gender,
            options          = mapOf(
                "M" to "남성",
                "F" to "여성",
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

// ── Inline Option Selector ────────────────────────────────────────────────────
// For ≤3 options where all choices should be visible without opening a modal.
// Tapping an already-selected option deselects it.
@Composable
private fun InlineOptionSelector(
    label: String,
    selectedKey: String,
    options: Map<String, String>,
    onOptionSelected: (String) -> Unit,
) {
    FieldLabel(label.uppercase())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, displayText) ->
            val isSelected = key == selectedKey
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue   = if (isPressed) 0.97f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                label         = "optionScale",
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceContainer,
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                else Color.Transparent,
                        shape = MaterialTheme.shapes.small,
                    )
                    .clickable(interactionSource = interactionSource, indication = null) {
                        onOptionSelected(if (isSelected) "" else key)
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    text  = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Sheet Option Selector ─────────────────────────────────────────────────────
// Trigger row that opens a ModalBottomSheet with a radio-style checklist.
// Auto-dismisses on selection — no confirm button needed.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetOptionSelector(
    label: String,
    selectedKey: String,
    options: Map<String, String>,
    onOptionSelected: (String) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    FieldLabel(label.uppercase())

    // Trigger row — matches visual weight of the other form fields
    val triggerInteraction = remember { MutableInteractionSource() }
    val triggerPressed by triggerInteraction.collectIsPressedAsState()
    val triggerScale by animateFloatAsState(
        targetValue   = if (triggerPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label         = "triggerScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(triggerScale)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interactionSource = triggerInteraction, indication = null) {
                showSheet = true
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = options[selectedKey] ?: "선택안함",
            style = MaterialTheme.typography.bodyLarge,
            color = if (selectedKey.isEmpty()) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector        = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.outline,
            modifier           = Modifier.size(20.dp),
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest  = { showSheet = false },
            sheetState        = sheetState,
            containerColor    = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape             = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            scrimColor        = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            // Sheet header
            Text(
                text     = label,
                style    = MaterialTheme.typography.titleMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
            )

            // "No selection" row
            SheetOptionRow(
                text       = "선택안함",
                isSelected = selectedKey.isEmpty(),
                onClick    = { onOptionSelected(""); showSheet = false },
            )

            options.forEach { (key, displayText) ->
                SheetOptionRow(
                    text       = displayText,
                    isSelected = key == selectedKey,
                    onClick    = { onOptionSelected(key); showSheet = false },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SheetOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
        if (isSelected) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

// ── Form field helpers ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState()

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC)
                        val y = dt.year.toString().padStart(4, '0')
                        @Suppress("DEPRECATION")
                        val m = dt.monthNumber.toString().padStart(2, '0')
                        @Suppress("DEPRECATION")
                        val d = dt.dayOfMonth.toString().padStart(2, '0')
                        onValueChange("$y.$m.$d")
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    TextField(
        value = value,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(8)
            val formatted = when {
                digits.length <= 4 -> digits
                digits.length <= 6 -> "${digits.take(4)}.${digits.drop(4)}"
                else -> "${digits.take(4)}.${digits.drop(4).take(2)}.${digits.drop(6)}"
            }
            onValueChange(formatted)
        },
        placeholder = { Text("2000.01.01") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    imageVector        = Icons.Default.CalendarMonth,
                    contentDescription = "날짜 선택",
                )
            }
        },
        modifier        = Modifier.fillMaxWidth(),
        singleLine      = true,
        shape           = MaterialTheme.shapes.small,
        isError         = error != null,
        supportingText  = error?.let { msg -> { Text(msg) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors          = TextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
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
    error: String? = null,
    focusRequester: FocusRequester? = null,
) {
    TextField(
        value           = value,
        onValueChange   = onValueChange,
        placeholder     = { Text(placeholder) },
        modifier        = Modifier
            .fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        singleLine      = true,
        shape           = MaterialTheme.shapes.small,
        isError         = error != null,
        supportingText  = error?.let { msg -> { Text(msg) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors          = TextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorContainerColor     = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor     = Color.Transparent,
        ),
    )
}

// Resolves a language-independent field error to localized text (null -> no error).
private fun AppStrings.messageFor(error: RegisterFieldError?): String? = when (error) {
    RegisterFieldError.REQUIRED              -> errorRequired
    RegisterFieldError.INVALID_EMAIL         -> errorInvalidEmail
    RegisterFieldError.PASSWORD_REQUIREMENTS -> errorPasswordRequirements
    RegisterFieldError.DATE_INCOMPLETE       -> errorDateIncomplete
    RegisterFieldError.DATE_INVALID          -> errorDateInvalid
    RegisterFieldError.INVALID_POSTCODE      -> errorInvalidPostcode
    RegisterFieldError.INVALID_CITY          -> errorInvalidCity
    RegisterFieldError.INVALID_HOUSE_NUMBER  -> errorInvalidHouseNumber
    null                                     -> null
}

// ── Phone Number Field ────────────────────────────────────────────────────────
// Country dial-code selector (flag + code) bound to a digits-only number input.
// Tapping the selector opens a searchable country bottom sheet.
@Composable
private fun PhoneNumberField(
    countryIso: String,
    number: String,
    onNumberChange: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val country = Countries.byIsoOrDefault(countryIso)

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Country selector chip
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue   = if (pressed) 0.97f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label         = "phoneCountryScale",
        )
        Row(
            modifier = Modifier
                .scale(scale)
                .height(56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(interactionSource = interaction, indication = null) { showSheet = true }
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(country.flag, style = MaterialTheme.typography.bodyLarge)
            Text(
                text  = "+${country.dialCode}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector        = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.outline,
                modifier           = Modifier.size(18.dp),
            )
        }

        // National number input (digits only — sanitized in the ViewModel)
        TextField(
            value           = number,
            onValueChange   = onNumberChange,
            placeholder     = { Text("170 1234567") },
            modifier        = Modifier.weight(1f),
            singleLine      = true,
            shape           = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors          = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }

    if (showSheet) {
        CountryPickerSheet(
            onDismiss  = { showSheet = false },
            onSelected = { iso -> onCountrySelected(iso); showSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val results = remember(query) { Countries.search(query) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        scrimColor       = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        Text(
            text     = "Select Country",
            style    = MaterialTheme.typography.titleMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 12.dp),
        )

        TextField(
            value           = query,
            onValueChange   = { query = it },
            placeholder     = { Text("Search") },
            leadingIcon     = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine      = true,
            shape           = MaterialTheme.shapes.small,
            modifier        = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors          = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            items(results, key = { it.iso }) { c ->
                CountryRow(country = c, onClick = { onSelected(c.iso) })
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CountryRow(country: Country, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(country.flag, style = MaterialTheme.typography.titleMedium)
        Text(
            text     = country.name,
            style    = MaterialTheme.typography.bodyLarge,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text  = "+${country.dialCode}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilledPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    criteria: PasswordCriteria,
    error: String? = null,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        TextField(
            value                = value,
            onValueChange        = onValueChange,
            placeholder          = { Text("Create a password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier             = Modifier
                .fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onFocusChanged { isFocused = it.isFocused },
            singleLine           = true,
            shape                = MaterialTheme.shapes.small,
            isError              = error != null,
            supportingText       = error?.let { msg -> { Text(msg) } },
            colors               = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor     = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor     = Color.Transparent,
            ),
        )

        // Checklist reveals once the user engages with the field — keeps the
        // resting form uncluttered while guiding active input.
        AnimatedVisibility(visible = isFocused || value.isNotEmpty()) {
            Column(
                modifier              = Modifier.padding(top = 12.dp, start = 4.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
            ) {
                PasswordCriterionRow(criteria.minLength, "At least 8 characters")
                PasswordCriterionRow(criteria.hasUpperAndLower, "Upper & lowercase letters")
                PasswordCriterionRow(criteria.hasDigit, "At least one number")
                PasswordCriterionRow(criteria.hasSpecial, "One special character")
                PasswordCriterionRow(criteria.notEmail, "Different from your email")
            }
        }
    }
}

// ── Password Criterion Row ────────────────────────────────────────────────────
// A single rule with an animated indicator: muted ✗ when unmet, green ✓ when met.
@Composable
private fun PasswordCriterionRow(met: Boolean, text: String) {
    val successColor = Color(0xFF22A06B)
    val iconTint by animateColorAsState(
        targetValue   = if (met) successColor else MaterialTheme.colorScheme.outline,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 250f),
        label         = "criterionTint",
    )
    val iconScale by animateFloatAsState(
        targetValue   = if (met) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label         = "criterionScale",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = if (met) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(16.dp).scale(iconScale),
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (met) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
