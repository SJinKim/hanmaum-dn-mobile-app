package com.hanmaum.dn.mobile.core.i18n

import androidx.compose.material3.CalendarLocale
import java.util.Locale

internal actual fun appCalendarLocale(languageTag: String): CalendarLocale = Locale.forLanguageTag(languageTag)
