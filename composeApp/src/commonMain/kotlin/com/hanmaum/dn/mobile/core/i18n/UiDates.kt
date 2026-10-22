package com.hanmaum.dn.mobile.core.i18n

import kotlinx.datetime.LocalDate

/** Display formatting only. The server's ISO value and time-zone interpretation stay unchanged. */
fun AppStrings.displayDate(iso: String): String = try {
    val date = LocalDate.parse(iso.take(10))
    nurtureFullDate(date.year, date.month.ordinal + 1, date.day)
} catch (_: IllegalArgumentException) {
    iso.take(10)
}
