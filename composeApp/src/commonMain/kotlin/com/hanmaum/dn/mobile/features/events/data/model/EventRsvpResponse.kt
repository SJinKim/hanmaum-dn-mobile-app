package com.hanmaum.dn.mobile.features.events.data.model

import kotlinx.serialization.Serializable

/** Wire shape of one item in `GET /events/rsvps/active`. */
@Serializable
data class EventRsvpResponse(
    val publicId: String,
    val title: String,
    val windowStart: String,
    val windowEnd: String,
    val announcementId: String? = null,
)
