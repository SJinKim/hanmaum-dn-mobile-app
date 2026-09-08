package com.hanmaum.dn.mobile.features.verse.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
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
     * Days that could ever be marked this week.
     *
     * For [VerseRecordKind.QUIET_TIME] the reading plan has no passage on
     * Sundays, so that day can never be filled and must not sit in the
     * denominator — a member who reads every single day would otherwise be
     * shown 6/7 for ever. Measured against the upstream, not assumed.
     */
    val markableDays: List<LocalDate> = when (kind) {
        VerseRecordKind.QUIET_TIME -> week.filter { it.dayOfWeek != DayOfWeek.SUNDAY }
        VerseRecordKind.RECITATION -> week
    }

    /** Marks that count toward [markableDays], so the ratio cannot exceed 1. */
    val markedThisWeek: Int = markedDays.count { it in markableDays }

    fun isMarkable(day: LocalDate): Boolean = day in markableDays

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
