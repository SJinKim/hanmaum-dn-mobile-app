package com.hanmaum.dn.mobile.features.events.presentation

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Deadline wording for the RSVP screens.
 *
 * Everything renders in the device's own zone, matching how the rest of the app
 * treats server timestamps. Members sit in two countries, so a deadline is
 * deliberately shown as *their* local date.
 */
internal object RsvpFormat {

    private val koreanDay = mapOf(
        DayOfWeek.MONDAY to "월",
        DayOfWeek.TUESDAY to "화",
        DayOfWeek.WEDNESDAY to "수",
        DayOfWeek.THURSDAY to "목",
        DayOfWeek.FRIDAY to "금",
        DayOfWeek.SATURDAY to "토",
        DayOfWeek.SUNDAY to "일",
    )

    /** "8월 30일 (토)" */
    fun date(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.month.ordinal + 1}월 ${local.day}일 (${koreanDay[local.dayOfWeek].orEmpty()})"
    }

    /** "8월 30일" — no weekday, for the tighter reminder line. */
    fun shortDate(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.month.ordinal + 1}월 ${local.day}일"
    }

    /** "응답 마감 8월 30일 (토)" */
    fun deadline(instant: Instant): String = "응답 마감 ${date(instant)}"

    /** "8월 24일 응답" */
    fun respondedOn(instant: Instant): String = "${shortDate(instant)} 응답"

    /**
     * "D-3", or "D-DAY" on the closing day.
     *
     * Counted in whole local days rather than 24-hour blocks: a deadline tonight
     * and one tomorrow morning are a day apart to a reader, even when the clock
     * puts them fourteen hours apart.
     */
    fun countdown(windowEnd: Instant, now: Instant = Clock.System.now()): String {
        val zone = TimeZone.currentSystemDefault()
        val days = windowEnd.toLocalDateTime(zone).date.toEpochDays() -
            now.toLocalDateTime(zone).date.toEpochDays()
        return if (days <= 0) "D-DAY" else "D-$days"
    }

    /** "미정 · 8월 29일에 한 번 더 알림" */
    fun reminderHint(nextReminderAt: Instant): String =
        "미정 · ${shortDate(nextReminderAt)}에 한 번 더 알림"
}
