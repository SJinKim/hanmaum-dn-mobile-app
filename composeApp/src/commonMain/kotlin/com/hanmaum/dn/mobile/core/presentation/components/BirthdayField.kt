package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayField(
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

    // Drive the field with a TextFieldValue so we can keep the caret at the end
    // after the "." separators are auto-inserted. With a plain String value the
    // caret offset is preserved across the reformat and lands *before* the digit
    // just typed (e.g. "1987.|1"), so the next digit is inserted ahead of it and
    // the month/day digits get transposed ("198712" → "198721").
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (fieldValue.text != value) {
        // External change (e.g. the date picker) — resync, caret at end.
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }

    TextField(
        value = fieldValue,
        onValueChange = { raw ->
            val digits = raw.text.filter { it.isDigit() }.take(8)
            val formatted = when {
                digits.length <= 4 -> digits
                digits.length <= 6 -> "${digits.take(4)}.${digits.drop(4)}"
                else -> "${digits.take(4)}.${digits.drop(4).take(2)}.${digits.drop(6)}"
            }
            fieldValue = TextFieldValue(formatted, TextRange(formatted.length))
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
