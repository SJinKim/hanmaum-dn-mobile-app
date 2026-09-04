package com.hanmaum.dn.mobile.core.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The grid maths, previously private to `CalendarScreen` and never tested.
 * Extracting it for #164 made it reachable, and a wrong first-weekday shifts
 * every date in the month by a column without anything failing.
 */
class DnMonthCalendarTest {

    @Test
    fun daysInMonthCoversTheOrdinaryCases() {
        assertEquals(31, daysInMonth(2026, 1))
        assertEquals(30, daysInMonth(2026, 4))
        assertEquals(31, daysInMonth(2026, 8))
        assertEquals(31, daysInMonth(2026, 12))
    }

    @Test
    fun februaryFollowsTheGregorianLeapRule() {
        assertEquals(28, daysInMonth(2026, 2))
        assertEquals(29, daysInMonth(2024, 2), "divisible by 4")
        assertEquals(28, daysInMonth(1900, 2), "divisible by 100 but not 400")
        assertEquals(29, daysInMonth(2000, 2), "divisible by 400")
    }

    @Test
    fun theFirstOfAugust2026IsASaturday() {
        // Sunday-based index, so Saturday is 6. Verified against a real calendar,
        // not re-derived from the same formula.
        assertEquals(6, dayOfWeekIndex(2026, 8, 1))
    }

    @Test
    fun januaryAndFebruaryUseThePreviousYearInZeller() {
        // The month < 3 branch is the easiest part to get wrong.
        assertEquals(4, dayOfWeekIndex(2026, 1, 1), "1 Jan 2026 is a Thursday")
        assertEquals(0, dayOfWeekIndex(2026, 2, 1), "1 Feb 2026 is a Sunday")
    }

    @Test
    fun aLeapDayLandsWhereItShould() {
        assertEquals(4, dayOfWeekIndex(2024, 2, 29), "29 Feb 2024 is a Thursday")
    }

    @Test
    fun weekdaysAdvanceByOnePerDay() {
        var previous = dayOfWeekIndex(2026, 8, 1)
        for (day in 2..31) {
            val current = dayOfWeekIndex(2026, 8, day)
            assertEquals((previous + 1) % 7, current, "day $day")
            previous = current
        }
    }

    @Test
    fun padTwoAlwaysGivesTwoDigits() {
        assertEquals("01", pad2(1))
        assertEquals("09", pad2(9))
        assertEquals("10", pad2(10))
        assertEquals("31", pad2(31))
    }
}
