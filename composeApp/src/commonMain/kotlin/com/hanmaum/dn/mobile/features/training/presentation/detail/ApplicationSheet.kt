package com.hanmaum.dn.mobile.features.training.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnSegmented
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.core.presentation.dismissKeyboardOnTap
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.login.presentation.components.BirthdayPickerField
import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.FormFieldOption
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingCourse
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureBadge

/**
 * The 양육 application form (#174), opened from 신청하기 on the detail page.
 *
 * The fields are whatever the chosen course asks for (`formFields`), started from the
 * member's profile. Figma: section `22 · 양육 신청 · Zustände`, boards `Formular`,
 * `Formular-Zustände` and `Dynamische Felder`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApplicationSheet(
    form: ApplicationFormState,
    onValueChange: (ApplicationField, String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = c.surface) {
        // A sheet has little empty space to tap, and the multi-line fields have no Done key
        // on iOS (#218): tapping anywhere outside a field closes the keyboard.
        Column(
            Modifier
                .fillMaxWidth()
                .dismissKeyboardOnTap()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(strings.nurtureApply, style = DnTheme.typography.title, color = c.textPrimary)
            Spacer(Modifier.height(12.dp))
            SelectedCourse(form.course)

            val outcome = form.outcome
            if (outcome is FormOutcome.Success) {
                Spacer(Modifier.height(28.dp))
                SuccessContent(outcome.courseName, onClose)
            } else {
                form.groups().forEach { (group, fields) ->
                    Spacer(Modifier.height(22.dp))
                    GroupHeader(group)
                    fields.forEach { ui ->
                        Spacer(Modifier.height(14.dp))
                        FormField(ui, form, onValueChange)
                    }
                }

                Spacer(Modifier.height(22.dp))
                ConsentRow(checked = form.consent, onChange = onConsentChange)

                if (outcome != null) {
                    Spacer(Modifier.height(14.dp))
                    OutcomeBanner(outcome)
                }

                Spacer(Modifier.height(16.dp))
                SubmitButton(form, onSubmit = onSubmit, onClose = onClose)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SelectedCourse(course: TrainingCourse) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnInnerShape)
            .background(c.surface2, DnInnerShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(strings.nurtureSelectedCourse, style = DnTheme.typography.label, color = c.textTertiary)
        Text(course.name, style = DnTheme.typography.captionStrong, color = c.textPrimary)
        course.dateText?.let { Text(it, style = DnTheme.typography.caption, color = c.textSecondary) }
    }
}

@Composable
private fun GroupHeader(group: ApplicationField.Group) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val (title, hint) = when (group) {
        ApplicationField.Group.BASIC -> strings.nurtureFormBasicGroup to strings.nurtureFormBasicHint
        ApplicationField.Group.CHURCH -> strings.nurtureFormChurchGroup to null
        ApplicationField.Group.EXPERIENCE -> strings.nurtureFormExperienceGroup to strings.nurtureFormExperienceHint
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = DnTheme.typography.captionStrong, color = c.textPrimary)
        hint?.let { Text(it, style = DnTheme.typography.label, color = c.textTertiary) }
    }
}

@Composable
private fun FormField(
    ui: FormFieldUi,
    form: ApplicationFormState,
    onValueChange: (ApplicationField, String) -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val label = ui.serverLabel ?: strings.labelFor(ui.field)
    val value = form.values[ui.field].orEmpty()
    if (ui.locked) {
        LockedInput(label, value)
        return
    }
    val error = form.errors[ui.field]
    val marker: @Composable () -> Unit = { RequiredMarker(ui.required) }
    val change: (String) -> Unit = { onValueChange(ui.field, it) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (ui.kind) {
            FieldKind.TEXT, FieldKind.EMAIL, FieldKind.PHONE -> DnTextField(
                label = label,
                value = value,
                onValueChange = change,
                modifier = Modifier.fillMaxWidth(),
                keyboardType = when (ui.kind) {
                    FieldKind.EMAIL -> KeyboardType.Email
                    FieldKind.PHONE -> KeyboardType.Phone
                    else -> KeyboardType.Text
                },
                isError = error != null,
                labelTrailing = marker,
            )

            FieldKind.DATE -> BirthdayPickerField(
                label = label,
                value = value,
                onValueChange = change,
                modifier = Modifier.fillMaxWidth(),
                placeholder = BIRTH_DATE_PLACEHOLDER,
                isError = error != null,
                labelTrailing = marker,
            )

            FieldKind.MULTILINE -> MultilineInput(
                label = label,
                marker = marker,
                value = value,
                placeholder = strings.nurtureFieldHistoryPlaceholder.takeIf { ui.field == ApplicationField.HISTORY },
                isError = error != null,
                onValueChange = change,
            )

            FieldKind.CHOICE -> ChoiceInput(
                field = ui.field,
                label = label,
                marker = marker,
                options = ui.options,
                value = value,
                isError = error != null,
                onValueChange = change,
            )
        }

        when {
            error != null -> FieldMessage(strings.messageFor(error), c.red)
            ui.field in form.missingFromProfile && value.isBlank() -> FieldMessage(strings.nurtureFormMissingInProfile, c.blue)
        }
    }
}

@Composable
private fun RequiredMarker(required: Boolean) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    if (required) {
        NurtureBadge(strings.nurtureFormRequired, c.blueDim, c.blue)
    } else {
        Text(strings.nurtureFormOptional, style = DnTheme.typography.label, color = c.textTertiary)
    }
}

/**
 * A profile value the form shows but may not change (이름, 생년월일), with the lock the
 * profile screen uses for church-managed fields.
 */
@Composable
private fun LockedInput(label: String, value: String) {
    val c = DnTheme.colors
    Column {
        FieldLabel(label, marker = {})
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(DnInnerShape)
                .background(c.surface2, DnInnerShape)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(value, style = DnTheme.typography.captionStrong, color = c.textSecondary, modifier = Modifier.weight(1f))
            Icon(DnIcons.Lock, null, tint = c.textTertiary, modifier = Modifier.size(15.dp))
        }
    }
}

/** Same label row as [DnTextField], for the inputs it does not cover. */
@Composable
private fun FieldLabel(label: String, marker: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = DnTheme.typography.label, color = DnTheme.colors.textTertiary)
        marker()
    }
}

@Composable
private fun MultilineInput(
    label: String,
    marker: @Composable () -> Unit,
    value: String,
    placeholder: String?,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    val c = DnTheme.colors
    Column {
        FieldLabel(label, marker)
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .clip(DnInnerShape)
                .background(c.surface2, DnInnerShape)
                .border(if (isError) 1.5.dp else 1.dp, if (isError) c.red else c.strokeSubtle, DnInnerShape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { if (it.length <= MULTILINE_MAX_LENGTH) onValueChange(it) },
                textStyle = DnTheme.typography.captionStrong.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.lime),
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty() && placeholder != null) {
                Text(placeholder, style = DnTheme.typography.captionStrong, color = c.textTertiary)
            }
        }
    }
}

/** Up to three options as a segment; more open a sheet with the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceInput(
    field: ApplicationField,
    label: String,
    marker: @Composable () -> Unit,
    options: List<FormFieldOption>,
    value: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Column {
        FieldLabel(label, marker)
        Spacer(Modifier.height(7.dp))

        if (options.size <= SEGMENT_MAX_OPTIONS) {
            DnSegmented(
                options = options.map { strings.optionLabel(field, it) },
                selectedIndex = options.indexOfFirst { it.value == value },
                onSelect = { onValueChange(options[it].value) },
                modifier = Modifier.fillMaxWidth(),
            )
            return@Column
        }

        var open by remember { mutableStateOf(false) }
        val selected = options.firstOrNull { it.value == value }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(DnInnerShape)
                .background(c.surface2, DnInnerShape)
                .border(if (isError) 1.5.dp else 1.dp, if (isError) c.red else c.strokeSubtle, DnInnerShape)
                .clickable { open = true }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selected?.let { strings.optionLabel(field, it) } ?: strings.nurtureFormChoose,
                style = DnTheme.typography.captionStrong,
                color = if (selected != null) c.textPrimary else c.textTertiary,
                modifier = Modifier.weight(1f),
            )
            Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(16.dp))
        }

        if (open) {
            ModalBottomSheet(
                onDismissRequest = { open = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = c.surface,
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text(label, style = DnTheme.typography.headline, color = c.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    options.forEach { option ->
                        val on = option.value == value
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = on,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onValueChange(option.value)
                                        open = false
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CheckMark(on)
                            Text(strings.optionLabel(field, option), style = DnTheme.typography.caption, color = c.textPrimary)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** Round mark for both the option list and the consent row; the radius tokens start at 18 dp. */
@Composable
private fun CheckMark(checked: Boolean) {
    val c = DnTheme.colors
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(c.lime)
                else Modifier.border(1.5.dp, c.strokeStrong, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(DnIcons.Check, null, tint = c.onLime, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun FieldMessage(text: String, ink: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(DnIcons.AlertTriangle, null, tint = ink, modifier = Modifier.size(12.dp))
        Text(text, style = DnTheme.typography.label, color = ink)
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnInnerShape)
            .background(c.surface2, DnInnerShape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckMark(checked)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(strings.nurtureFormConsent, style = DnTheme.typography.captionStrong, color = c.textPrimary)
            Text(strings.nurtureFormConsentBody, style = DnTheme.typography.label, color = c.textTertiary)
        }
    }
}

/** Board `Formular-Zustände`, states 7 to 13. */
@Composable
private fun OutcomeBanner(outcome: FormOutcome) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    data class Banner(val icon: ImageVector, val title: String, val body: String, val error: Boolean)

    val banner = when (outcome) {
        FormOutcome.Unavailable -> Banner(DnIcons.Hourglass, strings.nurtureUnavailable, strings.nurtureFormKeptInput, error = false)
        FormOutcome.AlreadyApplied -> Banner(DnIcons.Check, strings.nurtureFormAlreadyTitle, strings.nurtureFormAlreadyBody, error = false)
        FormOutcome.Full -> Banner(DnIcons.AlertTriangle, strings.nurtureFormFullTitle, strings.nurtureFormFullBody, error = true)
        FormOutcome.Closed -> Banner(DnIcons.AlertTriangle, strings.nurtureFormClosedTitle, strings.nurtureFormClosedBody, error = true)
        is FormOutcome.NotEligible -> Banner(
            DnIcons.AlertTriangle,
            strings.nurtureFormNotEligibleTitle,
            outcome.message ?: strings.nurtureCourseNotEligible,
            error = true,
        )
        is FormOutcome.Invalid -> Banner(
            DnIcons.AlertTriangle,
            strings.nurtureFormInvalidTitle,
            outcome.message ?: strings.nurtureFormInvalidBody,
            error = true,
        )
        FormOutcome.Failed -> Banner(DnIcons.AlertTriangle, strings.nurtureFormFailedTitle, strings.nurtureFormFailedBody, error = true)
        is FormOutcome.Success -> return
    }
    val ink = if (banner.error) c.red else c.textPrimary

    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnInnerShape)
            .background(if (banner.error) c.redDim else c.surface2, DnInnerShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(banner.icon, null, tint = if (banner.error) c.red else c.textSecondary, modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(banner.title, style = DnTheme.typography.captionStrong, color = ink)
            Text(banner.body, style = DnTheme.typography.label, color = c.textSecondary)
        }
    }
}

@Composable
private fun SubmitButton(form: ApplicationFormState, onSubmit: () -> Unit, onClose: () -> Unit) {
    val strings = LocalStrings.current
    val modifier = Modifier.fillMaxWidth()
    when (val outcome = form.outcome) {
        FormOutcome.AlreadyApplied -> DnPrimaryButton(label = strings.confirm, onClick = onClose, modifier = modifier)
        FormOutcome.Full -> DnPrimaryButton(strings.nurtureFormFull, onClick = {}, modifier = modifier, enabled = false, leading = DnIcons.UserCheck)
        FormOutcome.Closed -> DnPrimaryButton(strings.nurtureClosed, onClick = {}, modifier = modifier, enabled = false, leading = DnIcons.UserCheck)
        is FormOutcome.NotEligible ->
            DnPrimaryButton(strings.nurtureFormNotEligible, onClick = {}, modifier = modifier, enabled = false, leading = DnIcons.UserCheck)
        else -> DnPrimaryButton(
            label = when {
                form.isSubmitting -> strings.nurtureFormSubmitting
                outcome == FormOutcome.Failed -> strings.nurtureFormRetry
                else -> strings.nurtureApply
            },
            onClick = onSubmit,
            modifier = modifier,
            enabled = form.canSubmit,
            leading = DnIcons.UserCheck,
        )
    }
}

@Composable
private fun SuccessContent(courseName: String, onClose: () -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(48.dp)
                .clip(DnTileShape)
                .background(c.limeDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DnIcons.Check, null, tint = c.limeInk, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(strings.nurtureFormSuccessTitle, style = DnTheme.typography.headline, color = c.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            strings.nurtureFormSuccessBody(courseName),
            style = DnTheme.typography.caption,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        DnPrimaryButton(label = strings.confirm, onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}

private fun AppStrings.labelFor(field: ApplicationField): String = when (field) {
    ApplicationField.NAME -> nurtureFieldName
    ApplicationField.BIRTH_DATE -> nurtureFieldBirthDate
    ApplicationField.EMAIL -> nurtureFieldEmail
    ApplicationField.PHONE -> nurtureFieldPhone
    ApplicationField.GENDER -> nurtureFieldGender
    ApplicationField.BAPTIZED -> nurtureFieldBaptized
    ApplicationField.BAPTIZE_TYPE -> nurtureFieldBaptizeType
    ApplicationField.RESIDENCE -> nurtureFieldResidence
    ApplicationField.GYOGU -> nurtureFieldGyogu
    ApplicationField.SOON -> nurtureFieldSoon
    ApplicationField.CHILDREN -> nurtureFieldChildren
    ApplicationField.HISTORY -> nurtureFieldHistory
    ApplicationField.WAITING -> nurtureFieldWaiting
    ApplicationField.RUNNING -> nurtureFieldRunning
    ApplicationField.COMMENT -> nurtureFieldComment
}

/**
 * The server's option label when it sends one; otherwise the app's wording for the known
 * codes (ApplicationForms.KNOWN_OPTIONS); the bare value as a last resort.
 */
private fun AppStrings.optionLabel(field: ApplicationField, option: FormFieldOption): String =
    option.label ?: when (field) {
        ApplicationField.GENDER -> when (option.value) {
            "F" -> nurtureOptionFemale
            "M" -> nurtureOptionMale
            else -> null
        }
        ApplicationField.BAPTIZED -> when (option.value) {
            "1" -> nurtureOptionInfantBaptism
            "2" -> nurtureOptionConfirmation
            "3" -> nurtureOptionBaptism
            "4" -> nurtureOptionUnbaptized
            else -> null
        }
        ApplicationField.BAPTIZE_TYPE -> when (option.value) {
            "1" -> nurtureOptionInfantBaptism
            "2" -> nurtureOptionChildBaptism
            "3" -> nurtureOptionConfirmation
            "4" -> nurtureOptionBaptism
            "5" -> nurtureOptionUnbaptized
            else -> null
        }
        else -> null
    } ?: option.value

private fun AppStrings.messageFor(error: FieldError): String = when (error) {
    FieldError.Required -> nurtureFormErrorRequired
    FieldError.InvalidEmail -> nurtureFormErrorEmail
    FieldError.InvalidDate -> nurtureFormErrorDate
    FieldError.InvalidPhone -> nurtureFormErrorPhone
    FieldError.InvalidChoice -> nurtureFormErrorChoice
    is FieldError.Server -> error.message
}

private const val SEGMENT_MAX_OPTIONS = 3

/** TrainingApplicationRequest caps the free-text fields at 2000 characters. */
private const val MULTILINE_MAX_LENGTH = 2000

private const val BIRTH_DATE_PLACEHOLDER = "2000.01.01"
