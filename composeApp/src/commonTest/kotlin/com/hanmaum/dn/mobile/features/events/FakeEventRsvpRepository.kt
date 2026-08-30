package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvpCheckIn
import com.hanmaum.dn.mobile.features.events.domain.model.RespondResult
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository

class FakeEventRsvpRepository : EventRsvpRepository {
    var activeResult: Result<List<EventRsvp>> = Result.success(emptyList())

    /** Per-event override; falls back to echoing the requested status. */
    val respondResults: MutableMap<String, RespondResult> = mutableMapOf()
    val respondCalls = mutableListOf<Pair<String, RsvpStatus>>()

    var defaultCheckIn: CheckInResult =
        CheckInResult.Success(EventRsvpCheckIn("e1", "행사", "2026-07-12T10:00:00+09:00"))
    val checkInResults: MutableMap<String, CheckInResult> = mutableMapOf()
    var checkInCallCount = 0

    override suspend fun getActiveRsvps(): Result<List<EventRsvp>> = activeResult

    override suspend fun respond(publicId: String, status: RsvpStatus): RespondResult {
        respondCalls += publicId to status
        return respondResults[publicId] ?: RespondResult.Success(status)
    }

    override suspend fun checkIn(publicId: String): CheckInResult {
        checkInCallCount++
        return checkInResults[publicId] ?: defaultCheckIn
    }
}
