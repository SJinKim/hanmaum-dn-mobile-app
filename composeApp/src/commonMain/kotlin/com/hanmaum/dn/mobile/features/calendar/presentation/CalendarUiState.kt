package com.hanmaum.dn.mobile.features.calendar.presentation

import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent

enum class ViewMode { CALENDAR, LIST }

data class CalendarUiState(
    val year: Int = 2026,
    val month: Int = 1,
    val todayYear: Int = 2026,
    val todayMonth: Int = 1,
    val events: List<CalendarEvent> = emptyList(),
    val selectedDay: Int? = null,
    val selectedEvent: CalendarEvent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val viewMode: ViewMode = ViewMode.CALENDAR,
    val yearEvents: List<CalendarEvent> = emptyList(),
    val yearEventsLoaded: Boolean = false,
    val isYearLoading: Boolean = false,
)
