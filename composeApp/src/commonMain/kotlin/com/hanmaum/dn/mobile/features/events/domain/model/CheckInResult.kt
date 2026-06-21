package com.hanmaum.dn.mobile.features.events.domain.model

/** Typed outcome of a check-in, decoupling the ViewModel from Ktor/HTTP specifics. */
sealed interface CheckInResult {
    data class Success(val checkIn: EventRsvpCheckIn) : CheckInResult
    data object AlreadyRegistered : CheckInResult // server already has this member (409)
    data object WindowClosed : CheckInResult       // outside the RSVP window (400)
    data object Failed : CheckInResult             // any other failure
}
