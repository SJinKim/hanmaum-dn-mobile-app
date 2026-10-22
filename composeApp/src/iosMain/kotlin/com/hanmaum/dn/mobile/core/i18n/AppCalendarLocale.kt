package com.hanmaum.dn.mobile.core.i18n

import androidx.compose.material3.CalendarLocale
import platform.Foundation.NSLocale

internal actual fun appCalendarLocale(languageTag: String): CalendarLocale = NSLocale(languageTag)
