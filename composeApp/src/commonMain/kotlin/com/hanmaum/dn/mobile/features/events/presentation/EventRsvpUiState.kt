package com.hanmaum.dn.mobile.features.events.presentation

import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp

data class EventRsvpUiState(
    val events: List<EventRsvp> = emptyList(),     // active, not yet handled
    val visible: Boolean = false,                  // host sheet shown?
    val checkingInId: String? = null,              // publicId currently in flight
    val checkedInIds: Set<String> = emptySet(),    // rows showing "참석 완료 ✓"
    val rowErrors: Map<String, String> = emptyMap(),
)
