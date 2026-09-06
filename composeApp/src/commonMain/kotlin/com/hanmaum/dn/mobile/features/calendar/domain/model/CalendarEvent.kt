package com.hanmaum.dn.mobile.features.calendar.domain.model

data class CalendarEvent(
    /** Google's event id. Unique within one calendar, **not** across calendars. */
    val id: String,
    /** Which configured calendar this came from; with [id] it forms a stable list key. */
    val calendarId: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startDate: String,   // ISO-8601: "2026-06-01" (all-day) or "2026-06-01T10:00:00+02:00" (timed)
    val endDate: String,
    val isAllDay: Boolean,
) {
    /** Stable across calendars — use this wherever a list needs a key. */
    val key: String get() = "$calendarId/$id"
}
