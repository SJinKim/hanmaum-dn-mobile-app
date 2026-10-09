package com.hanmaum.dn.mobile.features.events.presentation

import com.hanmaum.dn.mobile.core.i18n.AppStrings
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

    fun date(instant: Instant, strings: AppStrings, zone: TimeZone = TimeZone.currentSystemDefault()): String =
        strings.rsvpDate(instant.toLocalDateTime(zone).date, includeWeekday = true)

    fun shortDate(instant: Instant, strings: AppStrings, zone: TimeZone = TimeZone.currentSystemDefault()): String =
        strings.rsvpDate(instant.toLocalDateTime(zone).date, includeWeekday = false)

    fun deadline(instant: Instant, strings: AppStrings, zone: TimeZone = TimeZone.currentSystemDefault()): String =
        strings.rsvpDeadline(date(instant, strings, zone))

    fun respondedOn(instant: Instant, strings: AppStrings, zone: TimeZone = TimeZone.currentSystemDefault()): String =
        strings.rsvpRespondedOn(shortDate(instant, strings, zone))

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

    fun reminderHint(nextReminderAt: Instant, strings: AppStrings, zone: TimeZone = TimeZone.currentSystemDefault()): String =
        strings.rsvpReminderHint(shortDate(nextReminderAt, strings, zone))
}
