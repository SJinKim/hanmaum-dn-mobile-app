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

    // ── normalisation on focus loss (#166) ───────────────────────────────

    @Test
    fun eightDigitsNormaliseDirectly() {
        assertEquals("2000.08.16", BirthDateInput.normalise("20000816"))
        assertEquals("2000.08.16", BirthDateInput.normalise("2000.08.16"))
    }

    @Test
    fun sevenDigitsResolveWhenOnlyOneReadingIsARealDate() {
        // 2000223: yyyyMdd gives 2000-02-23 (valid); yyyyMMd gives month 22.
        assertEquals("2000.02.23", BirthDateInput.normalise("2000223"))
        // 1987912: yyyyMdd gives 1987-09-12; yyyyMMd gives month 91.
        assertEquals("1987.09.12", BirthDateInput.normalise("1987912"))
    }

    @Test
    fun sevenDigitsWithASingleDigitDayAlsoResolve() {
        // 2000031: yyyyMdd reads month 0, which is no month; yyyyMMd reads
        // 2000-03-01 and survives. So the day is the short half here.
        assertEquals("2000.03.01", BirthDateInput.normalise("2000031"))
        // The other direction, for contrast: 2000312 is month 3 day 12,
        // because month 31 does not exist.
        assertEquals("2000.03.12", BirthDateInput.normalise("2000312"))
    }

    @Test
    fun anAmbiguousSevenDigitEntryIsLeftForTheUserToFinish() {
        // 2000105 is either 5 January or 10 May. Guessing would put a date in
        // the form that the user never typed.
        val out = BirthDateInput.normalise("2000105")
        assertNull(BirthDateInput.parse(out), "must not become a complete date")
        assertEquals("2000.10.5", out, "and stays exactly as typed")
    }

    @Test
    fun normalisingRejectsImpossibleCalendarDates() {
        // 2000431: yyyyMdd is 31 April, yyyyMMd is month 43. Neither survives,
        // so nothing is invented and the entry stays as typed.
        assertEquals("2000.43.1", BirthDateInput.normalise("2000431"))
        assertNull(BirthDateInput.parse(BirthDateInput.normalise("2000431")))
    }

    @Test
    fun theLeapRuleParticipatesInDisambiguation() {
        // 2001229: yyyyMdd is 29 February 2001, and 2001 is not a leap year;
        // yyyyMMd is month 22. Both fail, so the entry is not normalised — the
        // same digits in 2000 do resolve, which is the whole point.
        assertNull(BirthDateInput.parse(BirthDateInput.normalise("2001229")))
        assertEquals("2000.02.29", BirthDateInput.normalise("2000229"))
    }

    @Test
    fun aLeapDayResolvesInALeapYearOnly() {
        // 2000229: yyyyMdd 2000-02-29 valid (leap), yyyyMMd month 22 invalid.
        assertEquals("2000.02.29", BirthDateInput.normalise("2000229"))
    }

    @Test
    fun shortOrEmptyEntriesAreLeftAlone() {
        assertEquals("2000", BirthDateInput.normalise("2000"))
        assertEquals("2000.08", BirthDateInput.normalise("200008"))
        assertEquals("", BirthDateInput.normalise(""))
    }

    @Test
    fun normalisingIsIdempotent() {
        val once = BirthDateInput.normalise("2000223")
        assertEquals(once, BirthDateInput.normalise(once))
    }
}
