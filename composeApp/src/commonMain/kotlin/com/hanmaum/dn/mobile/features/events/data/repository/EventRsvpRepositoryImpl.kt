package com.hanmaum.dn.mobile.features.events.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpCheckInResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpResponse
import com.hanmaum.dn.mobile.features.events.data.model.SetRsvpResponseDto
import com.hanmaum.dn.mobile.features.events.data.model.SetRsvpResponseRequest
import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvpCheckIn
import com.hanmaum.dn.mobile.features.events.domain.model.RespondResult
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus
import com.hanmaum.dn.mobile.features.events.domain.repository.EventRsvpRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Instant

class EventRsvpRepositoryImpl(
    private val client: HttpClient,
) : EventRsvpRepository {

    override suspend fun getActiveRsvps(): Result<List<EventRsvp>> = runCatching {
        val response = client.get("events/rsvps/active")
        val body = response.body<ApiResponse<List<EventRsvpResponse>>>()
        body.data.orEmpty().mapNotNull { it.toDomainOrNull() }
    }

    // The global client uses expectSuccess = false, so non-2xx returns normally and we
    // branch on the status code. This keeps the ViewModel free of Ktor exception types.
    override suspend fun respond(publicId: String, status: RsvpStatus): RespondResult {
        val response = client.put("events/rsvps/$publicId/response") {
            contentType(ContentType.Application.Json)
            setBody(SetRsvpResponseRequest(status.name))
        }
        return when (response.status.value) {
            200, 201 -> response.body<ApiResponse<SetRsvpResponseDto>>().data
                ?.let { RsvpStatus.fromWire(it.status) }
                ?.let { RespondResult.Success(it) }
                ?: RespondResult.Failed
            400 -> RespondResult.WindowClosed
            else -> RespondResult.Failed
        }
    }

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

    /**
     * Drops an entry whose window cannot be parsed rather than failing the whole
     * list — one malformed row must not blank the screen.
     */
    private fun EventRsvpResponse.toDomainOrNull(): EventRsvp? {
        val start = windowStart.toInstantOrNull() ?: return null
        val end = windowEnd.toInstantOrNull() ?: return null
        return EventRsvp(
            publicId = publicId,
            title = title,
            windowStart = start,
            windowEnd = end,
            announcementId = announcementId,
            myStatus = RsvpStatus.fromWire(myStatus),
            respondedAt = respondedAt?.toInstantOrNull(),
            nextReminderAt = nextReminderAt?.toInstantOrNull(),
        )
    }

    private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

    private fun EventRsvpCheckInResponse.toDomain() =
        EventRsvpCheckIn(eventPublicId, eventTitle, checkedInAt)
}
