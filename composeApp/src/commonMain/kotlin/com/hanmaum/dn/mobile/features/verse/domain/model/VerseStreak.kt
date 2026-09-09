package com.hanmaum.dn.mobile.features.verse.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * One streak, as a card draws it: seven pills and a total.
 *
 * [weekStart] is the Sunday the shown week begins on, matching 주일 as the start
 * of the church week. [markedDays] holds only that week's dates — seven pills
 * are all the UI draws — while [totalDays] counts every mark ever.
 */
data class VerseStreak(
    val kind: VerseRecordKind,
    val weekStart: LocalDate,
    val markedDays: Set<LocalDate>,
    val todayMarked: Boolean,
    val todayMarkable: Boolean,
    val totalDays: Long,
) {
    /** The seven dates of the shown week, Sunday first. */
    val week: List<LocalDate> = (0..6).map { weekStart.plus(DatePeriod(days = it)) }

    /**
     * Marks that fall inside the shown week, so the ratio cannot exceed 1.
     *
     * All seven days count for both practices. The reading plan carries no
     * passage on Sundays, but the Sunday verses come from the sermon — someone
     * who goes to church and reads along has done the same thing they do on any
     * other day. Excluding it made a perfect week 6/7 by construction and told
     * those members their Sunday did not count (server #159).
     */
    val markedThisWeek: Int = markedDays.count { it in week }

    fun isMarked(day: LocalDate): Boolean = day in markedDays
}

/** Both streaks, as `/verses/records` returns them. */
data class VerseRecords(
    val quietTime: VerseStreak,
    val recitation: VerseStreak,
) {
    fun of(kind: VerseRecordKind): VerseStreak = when (kind) {
        VerseRecordKind.QUIET_TIME -> quietTime
        VerseRecordKind.RECITATION -> recitation
    }

    fun replacing(kind: VerseRecordKind, streak: VerseStreak): VerseRecords = when (kind) {
        VerseRecordKind.QUIET_TIME -> copy(quietTime = streak)
        VerseRecordKind.RECITATION -> copy(recitation = streak)
    }
}

/**
 * The streak as it will look once the server has accepted today's mark.
 *
 * Used to fill the pill before the request returns. With no undo available the
 * member has to see the tap land immediately; if the call then fails the caller
 * puts the old streak back.
 */
fun VerseStreak.withMark(day: LocalDate): VerseStreak = copy(
    markedDays = markedDays + day,
    todayMarked = true,
    totalDays = totalDays + 1,
)
