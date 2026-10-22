package com.hanmaum.dn.mobile.core.i18n

import androidx.compose.material3.ExperimentalMaterial3Api
import com.hanmaum.dn.mobile.features.login.domain.model.BirthDateInput
import com.hanmaum.dn.mobile.features.login.presentation.components.appDatePickerFormatter
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalMaterial3Api::class)
class BirthdayNativeLocaleTest {
    @Test fun foundationFormatsBothLanguagesRegardlessOfTheSuppliedDeviceLocale() {
        val selected = BirthDateInput.toEpochMillis("2000.01.01")
        val english = appCalendarLocale("en")
        val korean = appCalendarLocale("ko")
        assertTrue(appDatePickerFormatter(english).formatMonthYear(selected, korean).orEmpty().contains("January"))
        assertTrue(appDatePickerFormatter(korean).formatMonthYear(selected, english).orEmpty().contains("1월"))
    }
}
