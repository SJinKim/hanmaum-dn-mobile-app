package com.hanmaum.dn.mobile.features.calendar.domain.model

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startDate: String,   // ISO-8601: "2026-06-01" (all-day) or "2026-06-01T10:00:00+09:00" (timed)
    val endDate: String,
    val isAllDay: Boolean,
)
