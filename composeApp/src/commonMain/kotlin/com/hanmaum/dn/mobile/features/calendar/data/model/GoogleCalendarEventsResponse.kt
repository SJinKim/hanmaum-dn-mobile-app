package com.hanmaum.dn.mobile.features.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GoogleCalendarEventsResponse(
    val kind: String = "",
    val summary: String = "",
    val items: List<GoogleCalendarEventItem> = emptyList(),
    /**
     * Present as long as Google withheld part of the window. Modelling it is
     * what makes a month or year *complete*: without following it the app
     * silently shows the first page and calls that the calendar.
     */
    val nextPageToken: String? = null,
)

@Serializable
data class GoogleCalendarEventItem(
    val id: String,
    val summary: String = "",
    val description: String? = null,
    val location: String? = null,
    val start: GoogleCalendarDateTime,
    val end: GoogleCalendarDateTime,
    /** "confirmed" | "tentative" | "cancelled" — a cancelled instance must not render. */
    val status: String? = null,
)

@Serializable
data class GoogleCalendarDateTime(
    val date: String? = null,         // "2026-06-01" for all-day events
    val dateTime: String? = null,     // "2026-06-01T10:00:00+02:00" for timed events
)
