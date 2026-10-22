package com.hanmaum.dn.mobile.features.login.presentation

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.ExperimentalMaterial3Api
import com.hanmaum.dn.mobile.core.i18n.appCalendarLocale
import com.hanmaum.dn.mobile.features.login.domain.model.BirthDateInput
import com.hanmaum.dn.mobile.features.login.presentation.components.appDatePickerFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalMaterial3Api::class)
class BirthdayLocaleTest {
    private val selected = BirthDateInput.toEpochMillis("2000.01.01")

    @Test fun appChoiceWinsWhenMaterialPassesADifferentLocale() {
        val appLocale = appCalendarLocale("ko")
        val deviceLocale = appCalendarLocale("en")
        var receivedLocale: CalendarLocale? = null
        var accessibleDate = false
        val delegate = object : DatePickerFormatter {
            override fun formatMonthYear(monthMillis: Long?, locale: CalendarLocale): String {
                receivedLocale = locale
                assertEquals(selected, monthMillis)
                return "month"
            }
            override fun formatDate(dateMillis: Long?, locale: CalendarLocale, forContentDescription: Boolean): String {
                receivedLocale = locale
                accessibleDate = forContentDescription
                assertEquals(selected, dateMillis)
                return "date"
            }
        }
        val formatter = appDatePickerFormatter(appLocale, delegate)
        assertEquals("month", formatter.formatMonthYear(selected, deviceLocale))
        assertEquals(appLocale, receivedLocale)
        assertEquals("date", formatter.formatDate(selected, deviceLocale, true))
        assertEquals(appLocale, receivedLocale)
        assertTrue(accessibleDate)
    }

    @Test fun localeSpecificPickerStatePreservesTheEnteredBirthDate() {
        listOf("en", "ko").forEach { tag ->
            val state = DatePickerState(appCalendarLocale(tag), initialSelectedDateMillis = selected,
                yearRange = 1900..2026)
            assertEquals(selected, state.selectedDateMillis)
            assertEquals("2000.01.01", BirthDateInput.fromEpochMillis(state.selectedDateMillis!!))
        }
    }
}
