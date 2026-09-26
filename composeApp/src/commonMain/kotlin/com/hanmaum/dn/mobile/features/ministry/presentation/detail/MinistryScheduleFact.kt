package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistrySchedule

/**
 * The detail-fact rows for one schedule: "모임 시간 — 매주 토요일 16:00–18:00",
 * then "장소 — 본당". A described schedule labels its time row with the
 * description; a schedule with no day or time keeps the description as the value.
 */
internal data class ScheduleFact(val isPlace: Boolean, val label: String, val value: String)

internal fun MinistrySchedule.facts(strings: AppStrings): List<ScheduleFact> {
    val label = description?.takeIf { it.isNotBlank() }
    // dayHeaders starts on Sunday; DayOfWeek.ordinal starts on Monday.
    val day = dayOfWeek?.let { strings.nurtureWeekly(strings.dayHeaders[(it.ordinal + 1) % 7]) }
    val time = listOfNotNull(startTime.hhmm(), endTime.hhmm()).joinToString("–").ifEmpty { null }
    val `when` = listOfNotNull(day, time).joinToString(" ")
    val place = location?.takeIf { it.isNotBlank() }
    return listOfNotNull(
        when {
            `when`.isNotEmpty() -> ScheduleFact(false, label ?: strings.ministryMeetingTime, `when`)
            label != null -> ScheduleFact(false, strings.ministryMeetingTime, label)
            else -> null
        },
        place?.let { ScheduleFact(true, strings.ministryPlace, it) },
    )
}

private fun String?.hhmm(): String? = this?.takeIf { it.isNotBlank() }?.take(5)
