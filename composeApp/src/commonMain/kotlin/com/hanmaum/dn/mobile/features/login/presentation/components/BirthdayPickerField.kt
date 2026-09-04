package com.hanmaum.dn.mobile.features.login.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.features.login.domain.model.BirthDateInput
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Nobody registering is older than this, and nobody is born tomorrow. */
private const val EARLIEST_BIRTH_YEAR = 1900

/**
 * 생년월일 — typed first, picked second.
 *
 * The keyboard is the primary path: eight digits, dots inserted as you go, so
 * a birthday never costs a walk through decades of calendar. The calendar icon
 * is the secondary path and opens on whatever is already typed.
 *
 * The v1 screen had the typing half of this and lost it in the v2 makeover
 * (#154); the picker never opened on the entered date even in v1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayPickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    val strings = LocalStrings.current
    var showPicker by remember { mutableStateOf(false) }

    DnTextField(
        label = label,
        value = value,
        onValueChange = { onValueChange(BirthDateInput.format(it)) },
        modifier = modifier,
        placeholder = placeholder,
        trailing = DnIcons.Calendar,
        onTrailingClick = { showPicker = true },
        keyboardType = KeyboardType.Number,
    )

    if (showPicker) {
        val thisYear = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).year

        // Deliberately created inside the branch: the state is remembered only
        // while the dialog is on screen, so each open re-reads what has been
        // typed. Hoisting it would freeze the selection at whatever the field
        // held the first time the dialog ever appeared.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = BirthDateInput.toEpochMillis(value),
            yearRange = EARLIEST_BIRTH_YEAR..thisYear,
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onValueChange(BirthDateInput.fromEpochMillis(it))
                    }
                    showPicker = false
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                // Cancelling leaves whatever was typed exactly as it was.
                TextButton(onClick = { showPicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}
