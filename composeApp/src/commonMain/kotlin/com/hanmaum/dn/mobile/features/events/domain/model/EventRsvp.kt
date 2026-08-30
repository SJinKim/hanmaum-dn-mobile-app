package com.hanmaum.dn.mobile.features.events.domain.model

import kotlin.time.Instant

/**
 * An event the member is asked to answer for.
 *
 * [windowStart] and [windowEnd] are the *response* deadline, not the event
 * time — the congregation needs the headcount a few days ahead to plan. Never
 * present these as when the event happens.
 */
data class EventRsvp(
    val publicId: String,
    val title: String,
    val windowStart: Instant,
    val windowEnd: Instant,
    val announcementId: String?,
    /** null until the member has answered. */
    val myStatus: RsvpStatus? = null,
    val respondedAt: Instant? = null,
    /**
     * When the server will nag about a MAYBE again, or null when nothing is
     * pending. Comes from the server because only it knows the configured
     * reminder offsets — see hanmaum-dn-server#123.
     */
    val nextReminderAt: Instant? = null,
) {
    /**
     * MAYBE counts as pending on purpose: it is the state the server nags
     * about, so it has to stay somewhere the member can act on it.
     */
    val isPending: Boolean
        get() = myStatus == null || myStatus == RsvpStatus.MAYBE
}
