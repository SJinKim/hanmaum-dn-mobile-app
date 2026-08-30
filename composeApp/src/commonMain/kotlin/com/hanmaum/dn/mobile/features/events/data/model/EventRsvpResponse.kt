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
    /** null = not answered yet. Shipped with hanmaum-dn-server#124. */
    val myStatus: String? = null,
    val respondedAt: String? = null,
    /**
     * Next MAYBE reminder, or null when none is pending.
     *
     * NOT part of the server contract yet — it belongs to the reminder job in
     * hanmaum-dn-server#123 and is requested there. Until that ships the field
     * simply stays absent, and the reminder line is not drawn.
     */
    val nextReminderAt: String? = null,
)

/** Body of `PUT /events/rsvps/{publicId}/response` — server `EventRsvpResponseRequest`. */
@Serializable
data class SetRsvpResponseRequest(
    val status: String,
)

/**
 * Wire shape returned by the same endpoint — server `EventRsvpResponseDto`.
 *
 * Success is 200; 400 means the response window is shut. There is no 409: the
 * server upserts, so answering twice is not a conflict.
 */
@Serializable
data class SetRsvpResponseDto(
    val eventPublicId: String,
    val eventTitle: String,
    val status: String,
    val respondedAt: String? = null,
)
