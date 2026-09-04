package com.hanmaum.dn.mobile.features.login.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.login.presentation.components.BirthdayPickerField
import com.hanmaum.dn.mobile.features.login.presentation.components.PasswordCriteriaList
import org.koin.compose.viewmodel.koinViewModel

/**
 * 회원가입 — the fields the register endpoint actually accepts. Optional
 * extras (baptism, gender) were dropped from the first screen; they can be
 * filled in later from the profile.
 *
 * Address is optional as a whole but indivisible: the backend takes street and
 * house number as separate fields, and the validator requires both once either
 * is filled. The v2 screen rendered only the street, so filling it in produced
 * a required-field error on a field that did not exist and the button silently
 * did nothing (#155).
 */
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNavigateToPending: () -> Unit,
) {
    val viewModel: RegisterViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val c = DnTheme.colors
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            onNavigateToPending()
            viewModel.onNavigationHandled()
        }
    }

    // One requester per focusable field so a failed submit can jump to the
    // first invalid one. Focusing a field inside a scrolling column brings it
    // into view, which is the whole point: the offending field is usually off
    // screen when the button is tapped.
    val requesters = remember { RegisterField.entries.associateWith { FocusRequester() } }
    LaunchedEffect(state.focusTarget) {
        state.focusTarget?.let { target ->
            requesters[target]?.requestFocus()
            viewModel.onFocusHandled()
        }
    }

    /** Closes the keyboard, then submits. */
    val submit = {
        focusManager.clearFocus()
        keyboard?.hide()
        viewModel.register()
    }

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.registerTitle, onBack = onBackClick, actionIcon = null)

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // Tapping anywhere off a field dismisses the keyboard. iOS
                    // number pads have no Done key of their own, so without
                    // this the birth date and postcode trap the keyboard open.
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(18.dp))
                Text(strings.registerHeadline, style = DnTheme.typography.titleLg, color = c.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(strings.registerSubtitle, style = DnTheme.typography.caption, color = c.textSecondary)

                Spacer(Modifier.height(18.dp))
                state.bannerError?.let { banner -> FormBanner(banner, strings) }

                Text(
                    strings.registerRequiredLegend,
                    style = DnTheme.typography.caption,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(14.dp))

                // ── name ──
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        DnTextField(
                            required(strings.fieldLastName), state.lastName, viewModel::onLastNameChange,
                            Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.LAST_NAME)),
                            placeholder = strings.fieldLastName,
                            isError = state.lastNameError != null,
                        )
                        FieldError(strings.messageFor(state.lastNameError))
                    }
                    Column(Modifier.weight(1f)) {
                        DnTextField(
                            required(strings.fieldFirstName), state.firstName, viewModel::onFirstNameChange,
                            Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.FIRST_NAME)),
                            placeholder = strings.fieldFirstName,
                            isError = state.firstNameError != null,
                        )
                        FieldError(strings.messageFor(state.firstNameError))
                    }
                }

                Spacer(Modifier.height(16.dp))
                DnTextField(
                    required(strings.fieldEmail), state.email, viewModel::onEmailChange,
                    Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.EMAIL)),
                    placeholder = "hello@hanmaum.de", leading = DnIcons.Mail,
                    keyboardType = KeyboardType.Email, isError = state.emailError != null,
                )
                FieldError(strings.messageFor(state.emailError))

                Spacer(Modifier.height(16.dp))
                // The sanitiser keeps digits only, so the placeholder shows
                // digits only — it used to read "+49 …" and the + vanished as
                // the user typed it.
                DnTextField(
                    strings.fieldPhone, state.phoneNumber, viewModel::onPhoneChange,
                    Modifier.fillMaxWidth(), placeholder = "151 23456789",
                    keyboardType = KeyboardType.Phone,
                )

                Spacer(Modifier.height(16.dp))
                DnTextField(
                    required(strings.fieldPassword), state.password, viewModel::onPasswordChange,
                    Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.PASSWORD)),
                    placeholder = "••••••••", leading = DnIcons.Lock, trailing = DnIcons.Eye,
                    isPassword = true, keyboardType = KeyboardType.Password,
                    isError = state.passwordError != null,
                )
                FieldError(strings.messageFor(state.passwordError))
                // Shown once there is something to judge, so an untouched form
                // is not a wall of red crosses.
                if (state.password.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    PasswordCriteriaList(state.passwordCriteria, Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(16.dp))
                BirthdayPickerField(
                    strings.fieldBirthDate, state.birthDate, viewModel::onBirthDateChange,
                    Modifier.fillMaxWidth(), placeholder = "2000.01.01",
                )
                FieldError(strings.messageFor(state.birthDateError))

                // ── address: both halves or neither ──
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        DnTextField(
                            strings.fieldStreet, state.street, viewModel::onStreetChange,
                            Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.STREET)),
                            placeholder = "Musterstraße", isError = state.streetError != null,
                        )
                        FieldError(strings.messageFor(state.streetError))
                    }
                    Column(Modifier.width(104.dp)) {
                        DnTextField(
                            strings.fieldHouseNumber, state.houseNumber, viewModel::onHouseNumberChange,
                            Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.HOUSE_NUMBER)),
                            placeholder = "12a", isError = state.houseNumberError != null,
                        )
                        FieldError(strings.messageFor(state.houseNumberError))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.width(124.dp)) {
                        DnTextField(
                            required(strings.fieldZipCode), state.zipCode, viewModel::onZipChange,
                            Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.ZIP_CODE)),
                            placeholder = "40210", keyboardType = KeyboardType.Number,
                            isError = state.zipCodeError != null,
                        )
                        FieldError(strings.messageFor(state.zipCodeError))
                    }
                    Column(Modifier.weight(1f)) {
                        DnTextField(
                            required(strings.fieldCity), state.city, viewModel::onCityChange,
                            Modifier.fillMaxWidth().focusRequester(requesters.getValue(RegisterField.CITY)),
                            placeholder = "Düsseldorf", isError = state.cityError != null,
                            // Last field of the form: the key submits.
                            imeAction = ImeAction.Done,
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                        )
                        FieldError(strings.messageFor(state.cityError))
                    }
                }

                Spacer(Modifier.height(26.dp))
                DnPrimaryButton(
                    label = if (state.isLoading) strings.registerSubmitting else strings.registerSubmit,
                    onClick = submit,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

/** Marks a label as required with a glyph, never with colour alone. */
private fun required(label: String): String = "$label *"

/**
 * Top-of-form banner. Distinct from the per-field messages because the field
 * that failed is often scrolled out of sight when the button is tapped.
 */
@Composable
private fun FormBanner(banner: RegisterBanner, strings: AppStrings) {
    val c = DnTheme.colors
    // RegisteredPleaseLogin is a success that needs one more step, not a
    // failure — it must not read as red.
    val positive = banner is RegisterBanner.RegisteredPleaseLogin
    val shape = RoundedCornerShape(14.dp)
    val tint = if (positive) c.limeInk else c.red
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (positive) c.limeDim else c.redDim, shape)
            .border(1.dp, tint, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!positive) {
            Icon(DnIcons.AlertTriangle, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(
            text = when (banner) {
                is RegisterBanner.ServerMessage -> banner.text
                RegisterBanner.Generic -> strings.registerFailed
                RegisterBanner.RegisteredPleaseLogin -> strings.registerSuccessLogin
                RegisterBanner.MissingRequired -> strings.registerMissingRequired
            },
            style = DnTheme.typography.captionStrong,
            color = tint,
        )
    }
    Spacer(Modifier.height(14.dp))
}

/**
 * One field's validation message, or nothing at all.
 *
 * A composable rather than an inline `let` so every field renders the same
 * way — the redesign originally showed a message only for the birth date,
 * which quietly dropped the other seven the ViewModel already produces.
 */
@Composable
private fun FieldError(message: String?) {
    if (message == null) return
    Spacer(Modifier.height(6.dp))
    Text(message, style = DnTheme.typography.caption, color = DnTheme.colors.red)
}

/** Resolves a language-independent field error to localized text. */
private fun AppStrings.messageFor(error: RegisterFieldError?): String? = when (error) {
    RegisterFieldError.REQUIRED -> errorRequired
    RegisterFieldError.INVALID_EMAIL -> errorInvalidEmail
    RegisterFieldError.PASSWORD_REQUIREMENTS -> errorPasswordRequirements
    RegisterFieldError.DATE_INCOMPLETE -> errorDateIncomplete
    RegisterFieldError.DATE_INVALID -> errorDateInvalid
    RegisterFieldError.INVALID_POSTCODE -> errorInvalidPostcode
    RegisterFieldError.INVALID_CITY -> errorInvalidCity
    RegisterFieldError.INVALID_HOUSE_NUMBER -> errorInvalidHouseNumber
    null -> null
}
