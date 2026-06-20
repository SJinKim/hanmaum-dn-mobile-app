package com.hanmaum.dn.mobile.features.events.data.model

import kotlinx.serialization.Serializable

/** Wire shape of `POST /events/rsvps/{publicId}/check-in` data. */
@Serializable
data class EventRsvpCheckInResponse(
    val eventPublicId: String,
    val eventTitle: String,
    val checkedInAt: String,
)
