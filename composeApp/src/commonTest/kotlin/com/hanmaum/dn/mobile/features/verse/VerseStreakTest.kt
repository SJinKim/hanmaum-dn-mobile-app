package com.hanmaum.dn.mobile.features.verse

import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecordKind
import com.hanmaum.dn.mobile.features.verse.domain.model.withMark
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun `sunday counts for the quiet time streak too`() {
        // The reading plan has no passage on Sundays, but the verses come from
        // the sermon — reading along is the same practice as any other day, so
        // the day is markable and the denominator is seven (server #159).
        val qt = streak(VerseRecordKind.QUIET_TIME)

        assertEquals(7, qt.week.size)
        assertTrue(WEEK_START in qt.week)
    }

    @Test
    fun `both streaks span the same seven days`() {
        assertEquals(
            streak(VerseRecordKind.QUIET_TIME).week,
            streak(VerseRecordKind.RECITATION).week,
        )
    }

    @Test
    fun `a sunday mark counts toward the quiet time ratio`() {
        val qt = streak(
            VerseRecordKind.QUIET_TIME,
            marked = setOf(WEEK_START, LocalDate(2026, 9, 7), LocalDate(2026, 9, 8)),
        )

        assertEquals(3, qt.markedThisWeek)
    }

    @Test
    fun `a full week reads as seven of seven`() {
        val everyDay = (6..12).map { LocalDate(2026, 9, it) }.toSet()
        val qt = streak(VerseRecordKind.QUIET_TIME, marked = everyDay)

        assertEquals(7, qt.markedThisWeek)
        assertEquals(7, qt.week.size)
    }

    @Test
    fun `marks outside the shown week do not count`() {
        val qt = streak(VerseRecordKind.QUIET_TIME, marked = setOf(LocalDate(2026, 9, 5)))

        assertEquals(0, qt.markedThisWeek)
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
