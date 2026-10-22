package com.hanmaum.dn.mobile.features.login.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.CalendarLocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.i18n.appCalendarLocale
import com.hanmaum.dn.mobile.core.presentation.theme.AppSpacing
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import androidx.compose.foundation.layout.padding
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
    isError: Boolean = false,
    labelTrailing: (@Composable () -> Unit)? = null,
) {
    val strings = LocalStrings.current
    var showPicker by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }

    DnTextField(
        label = label,
        value = value,
        onValueChange = { onValueChange(BirthDateInput.format(it)) },
        // Settle the entry when the field is left, not on every keystroke:
        // rewriting mid-typing would fight the caret. Seven digits become a
        // date only when one reading of them is a real one (#166).
        modifier = modifier.onFocusChanged { focus ->
            if (hadFocus && !focus.hasFocus) onValueChange(BirthDateInput.normalise(value))
            hadFocus = focus.hasFocus
        },
        placeholder = placeholder,
        trailing = DnIcons.Calendar,
        onTrailingClick = { showPicker = true },
        keyboardType = KeyboardType.Number,
        isError = isError,
        labelTrailing = labelTrailing,
    )

    if (showPicker) {
        val thisYear = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).year

        // Deliberately created inside the branch: the state is remembered only
        // while the dialog is on screen, so each open re-reads what has been
        // typed. Hoisting it would freeze the selection at whatever the field
        // held the first time the dialog ever appeared.
        val calendarLocale = remember(strings.languageTag) { appCalendarLocale(strings.languageTag) }
        val pickerState = remember(calendarLocale) {
            DatePickerState(
                locale = calendarLocale,
                initialSelectedDateMillis = BirthDateInput.toEpochMillis(value),
                yearRange = EARLIEST_BIRTH_YEAR..thisYear,
            )
        }
        // Material's default headline reads the device locale separately from
        // DatePickerState. Pin every formatter call to the explicit app choice.
        val formatter = remember(calendarLocale) { appDatePickerFormatter(calendarLocale) }

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
            DatePicker(
                state = pickerState,
                dateFormatter = formatter,
                title = {
                    Text(strings.selectDate, modifier = Modifier.padding(AppSpacing.md),
                        style = DnTheme.typography.label)
                },
                headline = {
                    Text(
                        formatter.formatDate(pickerState.selectedDateMillis, calendarLocale) ?: strings.selectDate,
                        modifier = Modifier.padding(horizontal = AppSpacing.md),
                        style = DnTheme.typography.headline,
                    )
                },
                showModeToggle = false,
            )
        }
    }
}

/** Material may pass its device locale for the headline; the app's explicit choice wins. */
@OptIn(ExperimentalMaterial3Api::class)
internal fun appDatePickerFormatter(
    appLocale: CalendarLocale,
    formatter: DatePickerFormatter = DatePickerDefaults.dateFormatter(),
): DatePickerFormatter {
    return object : DatePickerFormatter {
        override fun formatMonthYear(monthMillis: Long?, locale: CalendarLocale): String? =
            formatter.formatMonthYear(monthMillis, appLocale)

        override fun formatDate(dateMillis: Long?, locale: CalendarLocale, forContentDescription: Boolean): String? =
            formatter.formatDate(dateMillis, appLocale, forContentDescription)
    }
}
