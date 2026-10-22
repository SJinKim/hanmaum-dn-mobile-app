package com.hanmaum.dn.mobile.core.i18n

import androidx.compose.material3.CalendarLocale

/** Keep platform locale construction out of shared presentation code. */
internal expect fun appCalendarLocale(languageTag: String): CalendarLocale
