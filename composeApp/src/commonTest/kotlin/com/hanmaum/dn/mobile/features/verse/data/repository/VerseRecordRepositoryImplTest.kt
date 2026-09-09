package com.hanmaum.dn.mobile.features.verse.data.repository

import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecordKind
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private const val RECORDS = """
{"success":true,"data":{
  "quietTime": {"weekStart":"2026-09-06","days":["2026-09-07","2026-09-08"],
                 "todayMarked":false,"todayMarkable":true,"totalDays":84},
  "recitation":{"weekStart":"2026-09-06","days":["2026-09-06"],
                 "todayMarked":true,"todayMarkable":true,"totalDays":127}}}
"""

/** Answers per HTTP method, so the 409 path can fall through to a fresh GET. */
private fun mockClient(
    onGet: Pair<String, HttpStatusCode> = RECORDS to HttpStatusCode.OK,
    onPost: Pair<String, HttpStatusCode> = RECORDS to HttpStatusCode.Created,
    record: MutableList<String>? = null,
): HttpClient = HttpClient(MockEngine { request ->
    record?.add("${request.method.value} ${request.url.encodedPath}")
    val (body, status) = if (request.method == HttpMethod.Post) onPost else onGet
    respond(
        content = body,
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

class VerseRecordRepositoryImplTest {

    @Test
    fun `maps both streaks with their weeks and totals`() = runTest {
        val records = VerseRecordRepositoryImpl(mockClient()).getRecords().getOrThrow()

        assertEquals(LocalDate(2026, 9, 6), records.quietTime.weekStart)
        assertEquals(setOf(LocalDate(2026, 9, 7), LocalDate(2026, 9, 8)), records.quietTime.markedDays)
        assertEquals(84, records.quietTime.totalDays)
        assertFalse(records.quietTime.todayMarked)
        assertEquals(127, records.recitation.totalDays)
        assertTrue(records.recitation.todayMarked)
    }

    @Test
    fun `the kind travels with each streak and both span seven days`() = runTest {
        val records = VerseRecordRepositoryImpl(mockClient()).getRecords().getOrThrow()

        assertEquals(VerseRecordKind.QUIET_TIME, records.quietTime.kind)
        assertEquals(VerseRecordKind.RECITATION, records.recitation.kind)
        // The Sunday in days[] counts for both now.
        assertEquals(7, records.recitation.week.size)
        assertEquals(7, records.quietTime.week.size)
        assertEquals(1, records.recitation.markedThisWeek)
    }

    @Test
    fun `a missing block becomes an unmarkable empty week`() = runTest {
        val body = """{"success":true,"data":{"quietTime":null,"recitation":null}}"""

        val records = VerseRecordRepositoryImpl(mockClient(body to HttpStatusCode.OK)).getRecords().getOrThrow()

        assertFalse(records.quietTime.todayMarkable)
        assertTrue(records.quietTime.markedDays.isEmpty())
        assertEquals(0, records.quietTime.totalDays)
    }

    @Test
    fun `a block without a week cannot be marked`() = runTest {
        // Nothing to draw seven pills against, so nothing to tap either.
        val body = """{"success":true,"data":{"quietTime":{"todayMarkable":true},"recitation":{}}}"""

        val records = VerseRecordRepositoryImpl(mockClient(body to HttpStatusCode.OK)).getRecords().getOrThrow()

        assertFalse(records.quietTime.todayMarkable)
    }

    @Test
    fun `marking returns the refreshed block for that kind`() = runTest {
        val body = """
            {"success":true,"data":{"weekStart":"2026-09-06","days":["2026-09-09"],
             "todayMarked":true,"todayMarkable":true,"totalDays":85}}
        """.trimIndent()

        val streak = VerseRecordRepositoryImpl(mockClient(onPost = body to HttpStatusCode.Created))
            .mark(VerseRecordKind.QUIET_TIME).getOrThrow()

        assertEquals(VerseRecordKind.QUIET_TIME, streak.kind)
        assertEquals(85, streak.totalDays)
        assertTrue(streak.todayMarked)
    }

    @Test
    fun `an already marked day is a success and re-reads the streak`() = runTest {
        // The member's intent already holds, so a 409 is not a failure. The 409
        // body carries no block, so the honest way to reflect it is a fresh read.
        val calls = mutableListOf<String>()
        val repo = VerseRecordRepositoryImpl(
            mockClient(onPost = "" to HttpStatusCode.Conflict, record = calls),
        )

        val streak = repo.mark(VerseRecordKind.RECITATION).getOrThrow()

        assertTrue(streak.todayMarked)
        assertEquals(127, streak.totalDays)
        assertEquals(listOf("POST /verses/records", "GET /verses/records"), calls)
    }

    @Test
    fun `a day the server refuses fails rather than pretending`() = runTest {
        // 400 means today cannot be marked for this kind — a weekday without a
        // plan entry, or a week with no chosen verse for 암송. No longer Sundays.
        val repo = VerseRecordRepositoryImpl(mockClient(onPost = "" to HttpStatusCode.BadRequest))

        assertTrue(repo.mark(VerseRecordKind.QUIET_TIME).isFailure)
    }

    @Test
    fun `a failed read fails rather than showing an empty streak`() = runTest {
        val repo = VerseRecordRepositoryImpl(mockClient("" to HttpStatusCode.InternalServerError))

        assertTrue(repo.getRecords().isFailure)
    }
}
