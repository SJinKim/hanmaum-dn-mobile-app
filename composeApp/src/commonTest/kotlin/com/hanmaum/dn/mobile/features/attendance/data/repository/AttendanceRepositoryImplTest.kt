package com.hanmaum.dn.mobile.features.attendance.data.repository

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    responseJson: String,
    onRequest: ((HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = HttpStatusCode.OK,
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

/**
 * The JSON below is the shape MemberAttendanceController actually serves —
 * copied from MemberAttendanceSummaryResponse / MemberAttendanceHistoryResponse
 * in hanmaum-dn-server, which run under default Jackson (camelCase, no naming
 * strategy). Asserting against a hand-typed body is the point: #129 shipped a
 * DTO whose name matched a stale spec and a default hid the mismatch.
 */
class AttendanceRepositoryImplTest {

    // ── summary ──────────────────────────────────────────────────────────

    private val summaryJson = """
        {"success":true,"data":{
          "monthAttended":3,"monthTotal":4,
          "yearAttended":30,"yearToDateTotal":36,"rate":0.8333333333333334}}
    """.trimIndent()

    @Test
    fun summaryMapsEveryCounter() = runTest {
        val s = AttendanceRepositoryImpl(mockClient(summaryJson)).getMySummary().getOrThrow()
        assertEquals(3, s.monthAttended)
        assertEquals(4, s.monthTotal)
        assertEquals(30, s.yearAttended)
        assertEquals(36, s.yearToDateTotal)
        assertEquals(0.8333333333333334, s.rate)
    }

    @Test
    fun summaryRateBecomesWholePercent() = runTest {
        val s = AttendanceRepositoryImpl(mockClient(summaryJson)).getMySummary().getOrThrow()
        assertEquals(83, s.ratePercent)
    }

    @Test
    fun summaryCallsTheMemberScopedPath() = runTest {
        // /me/... resolves the member from the JWT. A path carrying a member id
        // would be a different endpoint and a privacy problem.
        var path = ""
        val client = mockClient(summaryJson) { path = it.url.encodedPath }
        AttendanceRepositoryImpl(client).getMySummary()
        assertEquals("/me/attendance/summary", path)
    }

    @Test
    fun summaryFailsWhenACounterIsMissing() = runTest {
        // No defaults on the DTO: a renamed field must break here, not silently
        // read as zero and report a perfect month as 0/0.
        val json = """{"success":true,"data":{"monthAttended":3,"monthTotal":4,"rate":0.5}}"""
        assertTrue(AttendanceRepositoryImpl(mockClient(json)).getMySummary().isFailure)
    }

    @Test
    fun summaryFailsWhenDataIsNull() = runTest {
        val json = """{"success":true,"message":"nope","data":null}"""
        assertTrue(AttendanceRepositoryImpl(mockClient(json)).getMySummary().isFailure)
    }

    // ── history ──────────────────────────────────────────────────────────

    private val historyJson = """
        {"success":true,"data":{
          "from":"2026-06-06","to":"2026-09-04",
          "entries":[
            {"definitionPublicId":"d1","definitionTitle":"주일예배","date":"2026-08-31",
             "checkedIn":true,"checkedInAt":"2026-08-31T09:12:00Z"},
            {"definitionPublicId":"d1","definitionTitle":"주일예배","date":"2026-09-03",
             "checkedIn":false,"checkedInAt":null}
          ]}}
    """.trimIndent()

    @Test
    fun historyMapsEntriesAndEchoedRange() = runTest {
        val h = AttendanceRepositoryImpl(mockClient(historyJson)).getMyHistory().getOrThrow()
        assertEquals("2026-06-06", h.from)
        assertEquals("2026-09-04", h.to)
        assertEquals(2, h.entries.size)
        assertEquals("주일예배", h.entries.first().definitionTitle)
    }

    @Test
    fun historyIsNewestFirst() = runTest {
        // The server documents newest-first; the client sorts too, so a change
        // there cannot silently invert the 최근 출석 list.
        val h = AttendanceRepositoryImpl(mockClient(historyJson)).getMyHistory().getOrThrow()
        assertEquals("2026-09-03", h.entries[0].date)
        assertEquals("2026-08-31", h.entries[1].date)
    }

    @Test
    fun historyKeepsMissedOccurrences() = runTest {
        // A missed occurrence is derived server-side and carries checkedIn=false.
        // Dropping it would turn the list into "days I showed up" and lose the point.
        val h = AttendanceRepositoryImpl(mockClient(historyJson)).getMyHistory().getOrThrow()
        assertFalse(h.entries.single { it.date == "2026-09-03" }.checkedIn)
        assertTrue(h.entries.single { it.date == "2026-08-31" }.checkedIn)
    }

    @Test
    fun historyToleratesAnEmptyRange() = runTest {
        val json = """{"success":true,"data":{"from":"2026-06-06","to":"2026-09-04","entries":[]}}"""
        val h = AttendanceRepositoryImpl(mockClient(json)).getMyHistory().getOrThrow()
        assertTrue(h.entries.isEmpty())
    }

    @Test
    fun historyPassesTheRangeAsQueryParameters() = runTest {
        // The server caps a span at 366 days, so the calendar asks for a year
        // in one call instead of one request per month.
        var url = ""
        val client = mockClient(historyJson) { url = it.url.toString() }
        AttendanceRepositoryImpl(client).getMyHistory(from = "2025-09-01", to = "2026-09-05")
        assertTrue(url.contains("from=2025-09-01"), "url was $url")
        assertTrue(url.contains("to=2026-09-05"), "url was $url")
    }

    @Test
    fun historyOmitsTheRangeWhenNoneIsGiven() = runTest {
        // Then the server applies its own 90-day default.
        var url = ""
        val client = mockClient(historyJson) { url = it.url.toString() }
        AttendanceRepositoryImpl(client).getMyHistory()
        assertFalse(url.contains("from="), "url was $url")
        assertFalse(url.contains("to="), "url was $url")
    }

    @Test
    fun historyKeepsTheCheckInTime() = runTest {
        // #164 shows it in the list; #110 had dropped the field.
        val h = AttendanceRepositoryImpl(mockClient(historyJson)).getMyHistory().getOrThrow()
        assertEquals("2026-08-31T09:12:00Z", h.entries.single { it.date == "2026-08-31" }.checkedInAt)
        assertEquals(null, h.entries.single { it.date == "2026-09-03" }.checkedInAt)
    }

    @Test
    fun historyCallsTheMemberScopedPath() = runTest {
        var path = ""
        val client = mockClient(historyJson) { path = it.url.encodedPath }
        AttendanceRepositoryImpl(client).getMyHistory()
        assertEquals("/me/attendance", path)
    }
}
