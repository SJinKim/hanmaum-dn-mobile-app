package com.hanmaum.dn.mobile.features.events.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpCheckInResponse
import com.hanmaum.dn.mobile.features.events.data.model.EventRsvpResponse
import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    responseJson: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpClient = HttpClient(MockEngine {
    respond(
        content = responseJson,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

class EventRsvpRepositoryImplTest {

    @Test
    fun getActiveRsvps_parsesItemsIncludingNullableAnnouncementId() = runTest {
        val payload = testJson.encodeToString(
            ApiResponse(
                success = true,
                data = listOf(
                    EventRsvpResponse("e1", "여름 수련회", "2026-07-12T09:00:00+09:00", "2026-07-12T12:00:00+09:00", "ann-1"),
                    EventRsvpResponse("e2", "청년부 모임", "2026-07-12T14:00:00+09:00", "2026-07-12T16:00:00+09:00", null),
                ),
            ),
        )
        val result = EventRsvpRepositoryImpl(mockClient(payload)).getActiveRsvps()

        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertEquals("ann-1", list[0].announcementId)
        assertNull(list[1].announcementId)
    }

    @Test
    fun checkIn_201_returnsSuccess() = runTest {
        val payload = testJson.encodeToString(
            ApiResponse(success = true, data = EventRsvpCheckInResponse("e1", "여름 수련회", "2026-07-12T10:23:41+09:00")),
        )
        val result = EventRsvpRepositoryImpl(mockClient(payload, HttpStatusCode.Created)).checkIn("e1")

        assertTrue(result is CheckInResult.Success)
        assertEquals("여름 수련회", (result as CheckInResult.Success).checkIn.eventTitle)
    }

    @Test
    fun checkIn_409_returnsAlreadyRegistered() = runTest {
        val result = EventRsvpRepositoryImpl(mockClient("{}", HttpStatusCode.Conflict)).checkIn("e1")
        assertEquals(CheckInResult.AlreadyRegistered, result)
    }

    @Test
    fun checkIn_400_returnsWindowClosed() = runTest {
        val result = EventRsvpRepositoryImpl(mockClient("{}", HttpStatusCode.BadRequest)).checkIn("e1")
        assertEquals(CheckInResult.WindowClosed, result)
    }
}
