package com.hanmaum.dn.mobile.features.training.presentation

import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.features.training.domain.model.RegistrationWindow
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Wording for the 양육 list and detail.
 *
 * Timestamps render in the device's zone, like the RSVP deadlines: members sit in two
 * countries, and a deadline is shown as their own local date.
 */
internal object TrainingFormat {

    /** "신청 9월 1일 – 9월 21일", "신청 ~ 9월 21일", "상시 접수", or null when there is nothing to say. */
    fun window(
        strings: AppStrings,
        window: RegistrationWindow,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): String? {
        if (window.isAlwaysOpen) return strings.nurtureAlwaysOpen
        val start = window.startsAt?.let { monthDay(strings, it, zone) }
        val end = window.endsAt?.let { monthDay(strings, it, zone) }
        return when {
            start != null && end != null -> strings.nurtureWindow(start, end)
            end != null -> strings.nurtureWindowUntil(end)
            start != null -> strings.nurtureWindowFrom(start)
            else -> null
        }
    }

    /** "2026년 9월 15일" */
    fun appliedOn(
        strings: AppStrings,
        instant: Instant,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val date = instant.toLocalDateTime(zone).date
        return strings.nurtureFullDate(date.year, date.month.ordinal + 1, date.day)
    }

    /** "9월 7일 시작 · 4주" */
    fun period(strings: AppStrings, detail: TrainingDetail): String? = listOfNotNull(
        detail.startDate?.let { strings.nurtureStarts(monthDay(strings, it)) },
        detail.durationWeeks?.let { strings.nurtureWeeks(it) },
    ).joinToString(" · ").ifEmpty { null }

    /** "매주 일요일 · 14:00 · 60분" */
    fun schedule(strings: AppStrings, detail: TrainingDetail): String? = listOfNotNull(
        // dayHeaders starts on Sunday; DayOfWeek.ordinal starts on Monday.
        detail.weekday?.let { strings.nurtureWeekly(strings.dayHeaders[(it.ordinal + 1) % 7]) },
        detail.startTime?.let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" },
        detail.durationMinutes?.let { strings.nurtureMinutes(it) },
    ).joinToString(" · ").ifEmpty { null }

    private fun monthDay(strings: AppStrings, instant: Instant, zone: TimeZone): String =
        monthDay(strings, instant.toLocalDateTime(zone).date)

    private fun monthDay(strings: AppStrings, date: LocalDate): String =
        strings.nurtureMonthDay(date.month.ordinal + 1, date.day)
}
