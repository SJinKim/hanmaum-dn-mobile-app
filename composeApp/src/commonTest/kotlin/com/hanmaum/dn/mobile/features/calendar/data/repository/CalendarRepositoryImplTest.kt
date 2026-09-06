package com.hanmaum.dn.mobile.features.calendar.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val CALENDAR_ID = "hanmaum.dev@gmail.com"
private const val API_KEY = "test-api-key"

private val testJson = Json { ignoreUnknownKeys = true }

private fun event(id: String, start: String, timed: Boolean = false, status: String? = null): String {
    val field = if (timed) "dateTime" else "date"
    val statusField = status?.let { """"status":"$it",""" } ?: ""
    return """{"id":"$id",$statusField"summary":"주일예배","start":{"$field":"$start"},"end":{"$field":"$start"}}"""
}

private fun page(items: List<String>, nextPageToken: String? = null): String {
    val token = nextPageToken?.let { ""","nextPageToken":"$it"""" } ?: ""
    return """{"kind":"calendar#events","summary":"$CALENDAR_ID","items":[${items.joinToString(",")}]$token}"""
}

/** Records every outgoing request so the URL itself can be asserted on. */
private class Recorder {
    val requests = mutableListOf<HttpRequestData>()
    fun param(name: String, index: Int = 0): String? = requests[index].url.parameters[name]
}

private fun clientOf(recorder: Recorder, vararg bodies: String, status: HttpStatusCode = HttpStatusCode.OK) =
    HttpClient(MockEngine { request ->
        recorder.requests += request
        respond(
            content = bodies.getOrElse(recorder.requests.size - 1) { bodies.last() },
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }) {
        install(ContentNegotiation) { json(testJson) }
    }

private fun repositoryOf(
    client: HttpClient,
    calendarId: String = CALENDAR_ID,
    apiKey: String = API_KEY,
) = CalendarRepositoryImpl(
    client = client,
    calendarId = calendarId,
    apiKey = apiKey,
    zone = TimeZone.of("Europe/Berlin"),
)

class CalendarRepositoryImplTest {

    @Test
    fun `the calendar id travels as an encoded path segment`() = runTest {
        val recorder = Recorder()
        val repo = repositoryOf(clientOf(recorder, page(emptyList())))

        repo.getEvents(2026, 5).getOrThrow()

        val url = recorder.requests.single().url
        assertEquals("www.googleapis.com", url.host)
        // Decoded, the segment is the address itself — never a raw "@" split
        // across the path by hand-built string concatenation.
        assertEquals(
            listOf("calendar", "v3", "calendars", CALENDAR_ID, "events"),
            url.segments,
        )
        assertFalse(url.encodedPath.contains(" "))
    }

    @Test
    fun `a summer month asks for a window in Berlin local time`() = runTest {
        val recorder = Recorder()
        val repo = repositoryOf(clientOf(recorder, page(emptyList())))

        repo.getEvents(2026, 5).getOrThrow()

        // 1 May 2026 00:00 in Berlin is CEST, so 22:00 UTC on 30 April.
        assertEquals("2026-04-30T22:00:00Z", recorder.param("timeMin"))
        assertEquals("2026-05-31T22:00:00Z", recorder.param("timeMax"))
        assertEquals("Europe/Berlin", recorder.param("timeZone"))
    }

    @Test
    fun `a winter month follows the standard time offset`() = runTest {
        val recorder = Recorder()
        val repo = repositoryOf(clientOf(recorder, page(emptyList())))

        repo.getEvents(2026, 1).getOrThrow()

        // January is CET, one hour ahead of UTC.
        assertEquals("2025-12-31T23:00:00Z", recorder.param("timeMin"))
        assertEquals("2026-01-31T23:00:00Z", recorder.param("timeMax"))
    }

    @Test
    fun `december rolls the window into the next year`() = runTest {
        val recorder = Recorder()
        val repo = repositoryOf(clientOf(recorder, page(emptyList())))

        repo.getEvents(2026, 12).getOrThrow()

        assertEquals("2026-11-30T23:00:00Z", recorder.param("timeMin"))
        assertEquals("2026-12-31T23:00:00Z", recorder.param("timeMax"))
    }

    @Test
    fun `a year asks for the whole year in one window`() = runTest {
        val recorder = Recorder()
        val repo = repositoryOf(clientOf(recorder, page(emptyList())))

        repo.getYearEvents(2026).getOrThrow()

        assertEquals("2025-12-31T23:00:00Z", recorder.param("timeMin"))
        assertEquals("2026-12-31T23:00:00Z", recorder.param("timeMax"))
    }

    @Test
    fun `recurring events are expanded and ordered by start time`() = runTest {
        val recorder = Recorder()
        val repo = repositoryOf(clientOf(recorder, page(emptyList())))

        repo.getEvents(2026, 5).getOrThrow()

        assertEquals("true", recorder.param("singleEvents"))
        assertEquals("startTime", recorder.param("orderBy"))
        assertEquals(API_KEY, recorder.param("key"))
    }

    @Test
    fun `every page is followed until google stops handing out a token`() = runTest {
        val recorder = Recorder()
        val client = clientOf(
            recorder,
            page(listOf(event("a", "2026-05-03")), nextPageToken = "page-2"),
            page(listOf(event("b", "2026-05-10")), nextPageToken = "page-3"),
            page(listOf(event("c", "2026-05-17"))),
        )

        val events = repositoryOf(client).getEvents(2026, 5).getOrThrow()

        assertEquals(3, recorder.requests.size)
        assertEquals(listOf("a", "b", "c"), events.map { it.id })
        // Only the follow-up requests carry a token, and each carries the one
        // the previous page handed back.
        assertEquals(null, recorder.param("pageToken", 0))
        assertEquals("page-2", recorder.param("pageToken", 1))
        assertEquals("page-3", recorder.param("pageToken", 2))
    }

    @Test
    fun `results across pages come back sorted by start`() = runTest {
        val recorder = Recorder()
        val client = clientOf(
            recorder,
            page(listOf(event("late", "2026-05-24")), nextPageToken = "page-2"),
            page(listOf(event("early", "2026-05-02"))),
        )

        val events = repositoryOf(client).getEvents(2026, 5).getOrThrow()

        assertEquals(listOf("early", "late"), events.map { it.id })
    }

    @Test
    fun `a cancelled instance does not reach the screen`() = runTest {
        val recorder = Recorder()
        val client = clientOf(
            recorder,
            page(
                listOf(
                    event("kept", "2026-05-03"),
                    event("dropped", "2026-05-10", status = "cancelled"),
                ),
            ),
        )

        val events = repositoryOf(client).getEvents(2026, 5).getOrThrow()

        assertEquals(listOf("kept"), events.map { it.id })
    }

    @Test
    fun `all day and timed events are told apart`() = runTest {
        val recorder = Recorder()
        val client = clientOf(
            recorder,
            page(
                listOf(
                    event("allday", "2026-05-03"),
                    event("timed", "2026-05-03T10:00:00+02:00", timed = true),
                ),
            ),
        )

        val events = repositoryOf(client).getEvents(2026, 5).getOrThrow()

        assertTrue(events.first { it.id == "allday" }.isAllDay)
        assertFalse(events.first { it.id == "timed" }.isAllDay)
        assertEquals("2026-05-03T10:00:00+02:00", events.first { it.id == "timed" }.startDate)
    }

    @Test
    fun `an empty month is a success with no events`() = runTest {
        val recorder = Recorder()
        val result = repositoryOf(clientOf(recorder, page(emptyList()))).getEvents(2026, 5)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `an api error fails without ever naming the key`() = runTest {
        val recorder = Recorder()
        val client = clientOf(
            recorder,
            """{"error":{"code":403,"message":"denied"}}""",
            status = HttpStatusCode.Forbidden,
        )

        val result = repositoryOf(client).getEvents(2026, 5)

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertContains(message, "403")
        assertFalse(message.contains(API_KEY), "the API key must never surface in an error")
    }

    @Test
    fun `a missing calendar id fails before any request goes out`() = runTest {
        val recorder = Recorder()
        val result = repositoryOf(clientOf(recorder, page(emptyList())), calendarId = "").getEvents(2026, 5)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "GOOGLE_CALENDAR_ID")
        assertTrue(recorder.requests.isEmpty(), "a blank id must not become a real request")
    }

    @Test
    fun `a missing api key fails before any request goes out`() = runTest {
        val recorder = Recorder()
        val result = repositoryOf(clientOf(recorder, page(emptyList())), apiKey = "").getEvents(2026, 5)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "GOOGLE_CALENDAR_API_KEY")
        assertTrue(recorder.requests.isEmpty(), "a blank key must not become a real request")
    }
}
