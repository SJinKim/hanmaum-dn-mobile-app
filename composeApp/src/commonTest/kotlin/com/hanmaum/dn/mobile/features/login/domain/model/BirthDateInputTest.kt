package com.hanmaum.dn.mobile.features.login.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BirthDateInputTest {

    // ── typing ───────────────────────────────────────────────────────────

    @Test
    fun insertsTheDotsWhileTyping() {
        assertEquals("2", BirthDateInput.format("2"))
        assertEquals("2000", BirthDateInput.format("2000"))
        assertEquals("2000.0", BirthDateInput.format("20000"))
        assertEquals("2000.08", BirthDateInput.format("200008"))
        assertEquals("2000.08.1", BirthDateInput.format("2000081"))
        assertEquals("2000.08.16", BirthDateInput.format("20000816"))
    }

    @Test
    fun reformattingAnAlreadyFormattedValueIsStable() {
        // The field feeds its own output back in on every keystroke, so this
        // has to be a fixed point or the dots multiply.
        val once = BirthDateInput.format("20000816")
        assertEquals(once, BirthDateInput.format(once))
    }

    @Test
    fun ignoresEverythingThatIsNotADigit() {
        assertEquals("2000.08.16", BirthDateInput.format("2000-08-16"))
        assertEquals("2000.08.16", BirthDateInput.format("2000.08.16"))
        assertEquals("2000.08.16", BirthDateInput.format(" 2000 / 08 / 16 "))
    }

    @Test
    fun stopsAtEightDigits() {
        assertEquals("2000.08.16", BirthDateInput.format("200008169999"))
    }

    @Test
    fun deletingShortensTheValue() {
        assertEquals("2000.08.1", BirthDateInput.format("2000.08.1"))
        assertEquals("2000.0", BirthDateInput.format("2000.0"))
        assertEquals("", BirthDateInput.format(""))
    }

    // ── parsing ──────────────────────────────────────────────────────────

    @Test
    fun parsesACompleteDate() {
        assertEquals(LocalDate(2000, 8, 16), BirthDateInput.parse("2000.08.16"))
    }

    @Test
    fun refusesAnIncompleteDate() {
        assertNull(BirthDateInput.parse("2000.08"))
        assertNull(BirthDateInput.parse("2000.08.1"))
        assertNull(BirthDateInput.parse(""))
    }

    @Test
    fun refusesADayThatDoesNotExist() {
        assertNull(BirthDateInput.parse("2000.02.30"))
        assertNull(BirthDateInput.parse("2000.13.01"))
        assertNull(BirthDateInput.parse("2000.00.10"))
    }

    @Test
    fun handlesLeapYears() {
        assertEquals(LocalDate(2000, 2, 29), BirthDateInput.parse("2000.02.29"))
        assertEquals(LocalDate(2024, 2, 29), BirthDateInput.parse("2024.02.29"))
        // 1900 is divisible by 100 but not 400, so it is not a leap year
        assertNull(BirthDateInput.parse("1900.02.29"))
        assertNull(BirthDateInput.parse("2001.02.29"))
    }

    // ── picker round trip ────────────────────────────────────────────────

    @Test
    fun formatsADateBackIntoTheFieldShape() {
        assertEquals("2000.08.16", BirthDateInput.format(LocalDate(2000, 8, 16)))
        assertEquals("1987.01.02", BirthDateInput.format(LocalDate(1987, 1, 2)))
    }

    @Test
    fun survivesTheRoundTripThroughThePicker() {
        // Typed date -> millis handed to the picker -> confirmed value back.
        // A timezone slip anywhere in here lands the user a day early.
        for (text in listOf("2000.08.16", "1987.01.02", "2024.02.29", "1970.01.01", "1900.12.31")) {
            val millis = BirthDateInput.toEpochMillis(text)
            assertEquals(text, millis?.let { BirthDateInput.fromEpochMillis(it) }, "round trip for $text")
        }
    }

    @Test
    fun epochZeroIsTheFirstOfJanuary1970() {
        assertEquals("1970.01.01", BirthDateInput.fromEpochMillis(0L))
        assertEquals(0L, BirthDateInput.toEpochMillis("1970.01.01"))
    }

    @Test
    fun anIncompleteDateHasNoMillisForThePicker() {
        // The picker then opens on its own default instead of on nonsense.
        assertNull(BirthDateInput.toEpochMillis("2000.08"))
        assertNull(BirthDateInput.toEpochMillis(""))
        assertNull(BirthDateInput.toEpochMillis("2000.02.30"))
    }
}
