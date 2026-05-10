package com.hanmaum.dn.mobile.features.calendar.presentation

import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent

data class CalendarUiState(
    val year: Int = 2026,
    val month: Int = 1,
    val events: List<CalendarEvent> = emptyList(),
    val selectedDay: Int? = null,
    val selectedEvent: CalendarEvent? = null,   // for detail bottom sheet
    val isLoading: Boolean = false,
    val error: String? = null,
)
