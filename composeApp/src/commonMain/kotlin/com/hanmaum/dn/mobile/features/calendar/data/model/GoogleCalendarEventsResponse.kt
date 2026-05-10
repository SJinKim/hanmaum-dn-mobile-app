package com.hanmaum.dn.mobile.features.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GoogleCalendarEventsResponse(
    val kind: String = "",
    val summary: String = "",
    val items: List<GoogleCalendarEventItem> = emptyList(),
)

@Serializable
data class GoogleCalendarEventItem(
    val id: String,
    val summary: String = "",
    val description: String? = null,
    val location: String? = null,
    val start: GoogleCalendarDateTime,
    val end: GoogleCalendarDateTime,
)

@Serializable
data class GoogleCalendarDateTime(
    val date: String? = null,         // "2026-06-01" for all-day events
    val dateTime: String? = null,     // "2026-06-01T10:00:00+09:00" for timed events
)
