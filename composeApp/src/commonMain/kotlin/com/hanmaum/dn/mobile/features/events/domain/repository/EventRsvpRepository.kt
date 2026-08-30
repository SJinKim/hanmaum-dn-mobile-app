package com.hanmaum.dn.mobile.features.events.domain.repository

import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.RespondResult
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus

interface EventRsvpRepository {
    suspend fun getActiveRsvps(): Result<List<EventRsvp>>

    /**
     * Sets or changes the member's own answer. Idempotent on the server, so a
     * retry after a dropped connection cannot create a second answer.
     */
    suspend fun respond(publicId: String, status: RsvpStatus): RespondResult

    /**
     * Kept for the old check-in path. Superseded by [respond]; the server keeps
     * the endpoint alive for app versions that predate the status.
     */
    suspend fun checkIn(publicId: String): CheckInResult
}
