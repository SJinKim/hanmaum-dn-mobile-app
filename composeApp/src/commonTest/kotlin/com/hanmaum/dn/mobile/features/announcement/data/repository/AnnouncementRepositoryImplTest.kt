package com.hanmaum.dn.mobile.features.announcement.data.repository

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val testJson = Json { ignoreUnknownKeys = true }

private fun encodeAnnouncements(items: List<Announcement>): String =
    testJson.encodeToString(ListSerializer(Announcement.serializer()), items)

private fun mockClient(
    responseJson: String,
    onRequest: ((HttpRequestData) -> Unit)? = null
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
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

class AnnouncementRepositoryImplTest {

    private val announcement1 = Announcement(
        id = "1", title = "Notice Title", body = "Notice Body",
        startAt = "2024-01-01", endAt = null, isPinned = false, category = "NOTICE"
    )
    private val announcement2 = Announcement(
        id = "2", title = "Event Title", body = "Event Body",
        startAt = "2024-01-02", endAt = "2024-01-31", isPinned = true, category = "EVENT"
    )

    @Test
    fun getAnnouncements_returnsDeserializedList() = runTest {
        val client = mockClient(encodeAnnouncements(listOf(announcement1, announcement2)))
        val result = AnnouncementRepositoryImpl(client).getAnnouncements()

        assertEquals(2, result.size)
        assertEquals(announcement1, result[0])
        assertEquals(announcement2, result[1])
    }

    @Test
    fun getAnnouncements_returnsEmptyList() = runTest {
        val client = mockClient("[]")
        val result = AnnouncementRepositoryImpl(client).getAnnouncements()

        assertEquals(0, result.size)
    }

    @Test
    fun getAnnouncements_requestsCorrectPath() = runTest {
        var capturedPath = ""
        val client = mockClient("[]") { request -> capturedPath = request.url.encodedPath }
        AnnouncementRepositoryImpl(client).getAnnouncements()

        assertEquals("/announcements", capturedPath)
    }

    @Test
    fun getAnnouncementById_returnsMatchingAnnouncement() = runTest {
        val client = mockClient(encodeAnnouncements(listOf(announcement1, announcement2)))
        val result = AnnouncementRepositoryImpl(client).getAnnouncementById("2")

        assertEquals(announcement2, result)
    }

    @Test
    fun getAnnouncementById_returnsNullWhenNotFound() = runTest {
        val client = mockClient(encodeAnnouncements(listOf(announcement1, announcement2)))
        val result = AnnouncementRepositoryImpl(client).getAnnouncementById("nonexistent")

        assertNull(result)
    }

    @Test
    fun getAnnouncementById_makesExactlyOneNetworkRequest() = runTest {
        var requestCount = 0
        val client = mockClient(encodeAnnouncements(listOf(announcement1))) { requestCount++ }
        AnnouncementRepositoryImpl(client).getAnnouncementById("1")

        assertEquals(1, requestCount)
    }
}
