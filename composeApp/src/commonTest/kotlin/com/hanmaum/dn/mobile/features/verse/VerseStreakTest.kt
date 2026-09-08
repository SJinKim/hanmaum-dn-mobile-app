package com.hanmaum.dn.mobile.features.verse

import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecordKind
import com.hanmaum.dn.mobile.features.verse.domain.model.withMark
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerseStreakTest {

    @Test
    fun `the week runs seven days from its sunday`() {
        val week = streak(VerseRecordKind.RECITATION).week

        assertEquals(7, week.size)
        assertEquals(WEEK_START, week.first())
        assertEquals(LocalDate(2026, 9, 12), week.last())
    }

    @Test
    fun `the quiet time streak cannot be marked on a sunday`() {
        // Measured against the upstream: quiet-time.php has no passage on Sundays,
        // so that pill can never fill.
        val qt = streak(VerseRecordKind.QUIET_TIME)

        assertFalse(qt.isMarkable(WEEK_START))
        assertEquals(6, qt.markableDays.size)
    }

    @Test
    fun `the recitation streak can be marked any day of the week`() {
        val recite = streak(VerseRecordKind.RECITATION)

        assertTrue(recite.isMarkable(WEEK_START))
        assertEquals(7, recite.markableDays.size)
    }

    @Test
    fun `a sunday mark does not count toward the quiet time ratio`() {
        // Otherwise a member who reads every readable day would be shown 6 of 7
        // for ever, and marking the unmarkable day could push the ratio past one.
        val qt = streak(
            VerseRecordKind.QUIET_TIME,
            marked = setOf(WEEK_START, LocalDate(2026, 9, 7), LocalDate(2026, 9, 8)),
        )

        assertEquals(2, qt.markedThisWeek)
        assertEquals(6, qt.markableDays.size)
    }

    @Test
    fun `a full readable week reads as six of six`() {
        val everyReadableDay = (7..12).map { LocalDate(2026, 9, it) }.toSet()
        val qt = streak(VerseRecordKind.QUIET_TIME, marked = everyReadableDay)

        assertEquals(6, qt.markedThisWeek)
        assertEquals(6, qt.markableDays.size)
    }

    @Test
    fun `marking today fills the pill and raises the total`() {
        val before = streak(VerseRecordKind.RECITATION, totalDays = 41)

        val after = before.withMark(WEDNESDAY)

        assertTrue(after.isMarked(WEDNESDAY))
        assertTrue(after.todayMarked)
        assertEquals(42, after.totalDays)
    }
}
