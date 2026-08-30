package com.hanmaum.dn.mobile.features.events.presentation

import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus

data class EventRsvpUiState(
    val isLoading: Boolean = true,
    /** Every RSVP whose response window is open right now. */
    val events: List<EventRsvp> = emptyList(),
    /** Shown as the bottom sheet over Home; false once dismissed or answered. */
    val visible: Boolean = false,
    /** publicId of the answer currently in flight. */
    val respondingTo: String? = null,
    val rowErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
) {
    /** Unanswered and MAYBE, soonest deadline first. */
    val pending: List<EventRsvp>
        get() = events.filter { it.isPending }.sortedBy { it.windowEnd }

    val answered: List<EventRsvp>
        get() = events.filterNot { it.isPending }.sortedBy { it.windowEnd }

    val pendingCount: Int get() = pending.size
    val goingCount: Int get() = events.count { it.myStatus == RsvpStatus.GOING }
    val notGoingCount: Int get() = events.count { it.myStatus == RsvpStatus.NOT_GOING }
}
