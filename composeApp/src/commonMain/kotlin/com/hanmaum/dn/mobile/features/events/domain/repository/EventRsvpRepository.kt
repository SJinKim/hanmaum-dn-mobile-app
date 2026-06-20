package com.hanmaum.dn.mobile.features.events.domain.repository

import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp

interface EventRsvpRepository {
    suspend fun getActiveRsvps(): Result<List<EventRsvp>>
    suspend fun checkIn(publicId: String): CheckInResult
}
