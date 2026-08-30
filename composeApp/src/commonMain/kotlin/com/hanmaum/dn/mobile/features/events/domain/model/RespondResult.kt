package com.hanmaum.dn.mobile.features.events.domain.model

/** Typed outcome of setting an RSVP answer, keeping Ktor out of the ViewModel. */
sealed interface RespondResult {
    data class Success(val status: RsvpStatus) : RespondResult
    /** Outside the response window — the deadline passed while the screen was open. */
    data object WindowClosed : RespondResult
    data object Failed : RespondResult
}
