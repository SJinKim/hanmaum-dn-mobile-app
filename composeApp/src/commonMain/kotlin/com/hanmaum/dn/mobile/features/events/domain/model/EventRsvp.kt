package com.hanmaum.dn.mobile.features.events.domain.model

/** Active RSVP as shown on the mobile sheet. Times are ISO-8601 offset datetimes. */
data class EventRsvp(
    val publicId: String,
    val title: String,
    val windowStart: String,
    val windowEnd: String,
    val announcementId: String?,
)
