package com.hanmaum.dn.mobile.features.events.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpCheckInResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpResponse
import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvpCheckIn
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

class EventRsvpRepositoryImpl(
    private val client: HttpClient,
) : EventRsvpRepository {

    override suspend fun getActiveRsvps(): Result<List<EventRsvp>> = runCatching {
        val response = client.get("events/rsvps/active")
        val body = response.body<ApiResponse<List<EventRsvpResponse>>>()
        body.data?.map { it.toDomain() } ?: emptyList()
    }

    // The global client uses expectSuccess = false, so non-2xx returns normally and we
    // branch on the status code. This keeps the ViewModel free of Ktor exception types.
    override suspend fun checkIn(publicId: String): CheckInResult {
        val response = client.post("events/rsvps/$publicId/check-in")
        return when (response.status.value) {
            200, 201 -> response.body<ApiResponse<EventRsvpCheckInResponse>>().data
                ?.toDomain()
                ?.let { CheckInResult.Success(it) }
                ?: CheckInResult.Failed
            409 -> CheckInResult.AlreadyRegistered
            400 -> CheckInResult.WindowClosed
            else -> CheckInResult.Failed
        }
    }

    private fun EventRsvpResponse.toDomain() =
        EventRsvp(publicId, title, windowStart, windowEnd, announcementId)

    private fun EventRsvpCheckInResponse.toDomain() =
        EventRsvpCheckIn(eventPublicId, eventTitle, checkedInAt)
}
