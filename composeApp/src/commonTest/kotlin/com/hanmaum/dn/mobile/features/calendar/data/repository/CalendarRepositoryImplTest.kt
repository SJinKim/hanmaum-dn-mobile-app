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


private const val YOUTH_ID = "youth123@group.calendar.google.com"

/**
 * Routes by calendar id instead of by call order — with several calendars in
 * flight at once, call order is not something a test may rely on.
 */
private fun routedClient(
    recorder: Recorder,
    route: (calendarId: String, pageToken: String?) -> Pair<HttpStatusCode, String>,
) = HttpClient(MockEngine { request ->
    recorder.requests += request
    // .../calendars/{id}/events
    val id = request.url.segments[request.url.segments.indexOf("events") - 1]
    val (status, body) = route(id, request.url.parameters["pageToken"])
    respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
}

private fun repositoryOf(
    client: HttpClient,
    calendarIds: String = CALENDAR_ID,
    apiKey: String = API_KEY,
) = CalendarRepositoryImpl(
    client = client,
    calendarIds = calendarIds,
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
        val result = repositoryOf(clientOf(recorder, page(emptyList())), calendarIds = "").getEvents(2026, 5)

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

    @Test
    fun `both configured calendars are read and merged in time order`() = runTest {
        val recorder = Recorder()
        val client = routedClient(recorder) { id, _ ->
            HttpStatusCode.OK to when (id) {
                CALENDAR_ID -> page(listOf(event("main-01", "2026-09-01"), event("main-30", "2026-09-30")))
                YOUTH_ID -> page(listOf(event("youth-15", "2026-09-15"), event("youth-25", "2026-09-25")))
                else -> error("unexpected calendar $id")
            }
        }

        val events = repositoryOf(client, calendarIds = "$CALENDAR_ID,$YOUTH_ID").getEvents(2026, 9).getOrThrow()

        assertEquals(2, recorder.requests.size)
        // Interleaved by start, not grouped by source.
        assertEquals(listOf("main-01", "youth-15", "youth-25", "main-30"), events.map { it.id })
    }

    @Test
    fun `each event remembers which calendar it came from`() = runTest {
        val recorder = Recorder()
        val client = routedClient(recorder) { id, _ ->
            HttpStatusCode.OK to page(listOf(event("e1", "2026-09-15")))
        }

        val events = repositoryOf(client, calendarIds = "$CALENDAR_ID,$YOUTH_ID").getEvents(2026, 9).getOrThrow()

        assertEquals(setOf(CALENDAR_ID, YOUTH_ID), events.map { it.calendarId }.toSet())
    }

    // Google ids are unique per calendar, not globally. Two calendars handing
    // out the same id must stay two rows — a duplicate list key crashes Compose.
    @Test
    fun `the same event id on two calendars yields two distinct keys`() = runTest {
        val recorder = Recorder()
        val client = routedClient(recorder) { _, _ ->
            HttpStatusCode.OK to page(listOf(event("collision", "2026-09-15")))
        }

        val events = repositoryOf(client, calendarIds = "$CALENDAR_ID,$YOUTH_ID").getEvents(2026, 9).getOrThrow()

        assertEquals(2, events.size)
        assertEquals(2, events.map { it.key }.toSet().size)
    }

    // Dropping the calendar that failed and rendering the rest would look exactly
    // like a complete calendar — the failure #178 was filed for. It fails loudly.
    @Test
    fun `one failing calendar fails the whole load`() = runTest {
        val recorder = Recorder()
        val client = routedClient(recorder) { id, _ ->
            if (id == YOUTH_ID) HttpStatusCode.NotFound to """{"error":{"code":404}}"""
            else HttpStatusCode.OK to page(listOf(event("main-01", "2026-09-01")))
        }

        val result = repositoryOf(client, calendarIds = "$CALENDAR_ID,$YOUTH_ID").getEvents(2026, 9)

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertContains(message, YOUTH_ID)
        assertContains(message, "404")
        assertFalse(message.contains(API_KEY), "the API key must never surface in an error")
    }

    @Test
    fun `pagination is followed per calendar`() = runTest {
        val recorder = Recorder()
        val client = routedClient(recorder) { id, token ->
            HttpStatusCode.OK to when {
                id == YOUTH_ID && token == null ->
                    page(listOf(event("youth-a", "2026-09-15")), nextPageToken = "y2")
                id == YOUTH_ID -> page(listOf(event("youth-b", "2026-09-25")))
                else -> page(listOf(event("main-01", "2026-09-01")))
            }
        }

        val events = repositoryOf(client, calendarIds = "$CALENDAR_ID,$YOUTH_ID").getEvents(2026, 9).getOrThrow()

        assertEquals(3, recorder.requests.size)
        assertEquals(listOf("main-01", "youth-a", "youth-b"), events.map { it.id })
    }

    @Test
    fun `whitespace and empty entries in the config are ignored`() = runTest {
        val recorder = Recorder()
        val client = routedClient(recorder) { _, _ -> HttpStatusCode.OK to page(emptyList()) }

        repositoryOf(client, calendarIds = " $CALENDAR_ID , , $YOUTH_ID ,").getEvents(2026, 9).getOrThrow()

        val asked = recorder.requests.map { it.url.segments[it.url.segments.indexOf("events") - 1] }
        assertEquals(setOf(CALENDAR_ID, YOUTH_ID), asked.toSet())
        assertEquals(2, recorder.requests.size)
    }

    @Test
    fun `a single configured id still works unchanged`() = runTest {
        val recorder = Recorder()
        val client = routedClient(recorder) { _, _ -> HttpStatusCode.OK to page(listOf(event("only", "2026-09-15"))) }

        val events = repositoryOf(client).getEvents(2026, 9).getOrThrow()

        assertEquals(1, recorder.requests.size)
        assertEquals(listOf("only"), events.map { it.id })
    }

    @Test
    fun `a config of only separators fails before any request goes out`() = runTest {
        val recorder = Recorder()
        val result = repositoryOf(clientOf(recorder, page(emptyList())), calendarIds = " , ").getEvents(2026, 9)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "GOOGLE_CALENDAR_ID")
        assertTrue(recorder.requests.isEmpty())
    }
}
