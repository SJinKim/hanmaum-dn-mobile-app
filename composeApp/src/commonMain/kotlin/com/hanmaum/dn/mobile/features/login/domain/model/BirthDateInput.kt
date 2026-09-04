package com.hanmaum.dn.mobile.features.login.domain.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Typing a birth date, not navigating to one.
 *
 * A birthday is a date the user knows by heart, so the fast path has to be the
 * keyboard: eight digits, no separators to type. The calendar stays available
 * for the rare case where someone would rather point at it, and it opens on
 * whatever has already been typed.
 *
 * Everything here is pure so it can be tested without Compose — the field
 * itself only wires these functions to a text field and a dialog.
 */
object BirthDateInput {

    /** Length of a complete "YYYY.MM.DD". */
    const val LENGTH: Int = 10

    /**
     * Keeps the digits and puts the dots in, so typing `20000816` yields
     * `2000.08.16`. Anything that is not a digit is dropped, which also makes
     * pasting `2000-08-16` or `16.08.2000`-shaped junk predictable rather than
     * silently wrong: only the digit sequence counts.
     */
    fun format(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return when {
            digits.length <= 4 -> digits
            digits.length <= 6 -> "${digits.take(4)}.${digits.drop(4)}"
            else -> "${digits.take(4)}.${digits.drop(4).take(2)}.${digits.drop(6)}"
        }
    }

    /** `2000.08.16` for a date — the same shape [format] produces. */
    fun format(date: LocalDate): String = date.toString().replace('-', '.')

    /**
     * The date that was typed, or null when it is incomplete or not a real
     * calendar date. `LocalDate` does the calendar work, so 29 February is
     * accepted in 2000 and rejected in 2001 without a rule of our own.
     */
    fun parse(text: String): LocalDate? {
        if (text.length != LENGTH) return null
        return try {
            LocalDate.parse(text.replace('.', '-'))
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * UTC midnight for the typed date — the unit Material's date picker speaks.
     * UTC on both ends of the conversion, never the device zone: east of
     * Greenwich a local-midnight round trip lands on the previous day.
     */
    fun toEpochMillis(text: String): Long? =
        parse(text)?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()

    /** The date the picker returned, in the field's own format. */
    fun fromEpochMillis(millis: Long): String =
        format(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
}
