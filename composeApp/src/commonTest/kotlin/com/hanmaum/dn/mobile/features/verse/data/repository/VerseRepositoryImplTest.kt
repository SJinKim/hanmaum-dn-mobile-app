package com.hanmaum.dn.mobile.features.verse.data.repository

import com.hanmaum.dn.mobile.core.domain.repository.RememberedWeeklyVerse
import com.hanmaum.dn.mobile.core.domain.repository.VersePreferences
import com.hanmaum.dn.mobile.features.verse.FakeVersePreferences
import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerseState
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpClient = HttpClient(MockEngine {
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

private fun repository(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    preferences: VersePreferences = FakeVersePreferences(),
) = VerseRepositoryImpl(mockClient(body, status), preferences)

class VerseRepositoryImplTest {

    @Test
    fun `maps reference translation and link`() = runTest {
        val body = """
            {"success":true,"data":{"state":"PASSAGE",
              "reference":{"ko":"신명기 3:1-11","en":"Deuteronomy 3:1-11","de":"5. Mose 3,1-11"},
              "translation":"개역개정",
              "sourceUrl":"https://bible.asher.design/quiettime.php?qt_date=2026-09-08"}}
        """.trimIndent()

        val verse = repository(body).getTodayVerse().getOrThrow()

        assertEquals("신명기 3:1-11", verse?.referenceKo)
        assertEquals("Deuteronomy 3:1-11", verse?.referenceEn)
        assertEquals("개역개정", verse?.translation)
        assertEquals("https://bible.asher.design/quiettime.php?qt_date=2026-09-08", verse?.sourceUrl)
    }

    @Test
    fun `trims padding the upstream leaves on the fields`() = runTest {
        val body = """
            {"success":true,"data":{"state":"PASSAGE",
              "reference":{"ko":"  신명기 3:1-11  ","en":"  Deuteronomy 3:1-11 "},
              "translation":" 개역개정 ","sourceUrl":" https://example.org "}}
        """.trimIndent()

        val verse = repository(body).getTodayVerse().getOrThrow()

        assertEquals("신명기 3:1-11", verse?.referenceKo)
        assertEquals("Deuteronomy 3:1-11", verse?.referenceEn)
        assertEquals("개역개정", verse?.translation)
        assertEquals("https://example.org", verse?.sourceUrl)
    }

    @Test
    fun `one language alone still carries the card`() = runTest {
        val body = """{"success":true,"data":{"state":"PASSAGE","reference":{"ko":"시편 23:1"}}}"""

        val verse = repository(body).getTodayVerse().getOrThrow()

        assertEquals("시편 23:1", verse?.referenceKo)
        assertEquals("", verse?.referenceEn)
        assertNull(verse?.sourceUrl)
    }

    @Test
    fun `a row without any reference is dropped`() = runTest {
        val body = """{"success":true,"data":{"state":"PASSAGE","reference":{"ko":"","en":" "},"translation":"개역개정"}}"""

        assertNull(repository(body).getTodayVerse().getOrThrow())
    }

    @Test
    fun `an empty envelope means no passage today`() = runTest {
        val body = """{"success":true,"data":null}"""

        assertNull(repository(body).getTodayVerse().getOrThrow())
    }

    @Test
    fun `a gap in the plan hides the card`() = runTest {
        val body = """{"success":true,"data":{"state":"NO_PLAN"}}"""

        assertNull(repository(body).getTodayVerse().getOrThrow())
    }

    @Test
    fun `a sunday survives without a reference`() = runTest {
        // There is no planned passage on a Sunday, but the day is still markable
        // and the card names the service — so this row must not be dropped.
        val body = """{"success":true,"data":{"state":"SUNDAY_SERVICE"}}"""

        val verse = repository(body).getTodayVerse().getOrThrow()

        assertEquals(DailyVerseState.SUNDAY_SERVICE, verse?.state)
        assertEquals("", verse?.referenceKo)
    }

    @Test
    fun `a state this build does not know hides the card`() = runTest {
        val body = """{"success":true,"data":{"state":"FUTURE_THING"}}"""

        assertNull(repository(body).getTodayVerse().getOrThrow())
    }

    @Test
    fun `a passage carries its state through`() = runTest {
        val body = """{"success":true,"data":{"state":"PASSAGE","reference":{"ko":"시편 23:1"}}}"""

        val verse = repository(body).getTodayVerse().getOrThrow()

        assertEquals(DailyVerseState.PASSAGE, verse?.state)
    }

    @Test
    fun `no content means no passage today`() = runTest {
        val repo = repository("", HttpStatusCode.NoContent)

        assertNull(repo.getTodayVerse().getOrThrow())
    }

    @Test
    fun `not found means no passage today`() = runTest {
        val repo = repository("", HttpStatusCode.NotFound)

        assertNull(repo.getTodayVerse().getOrThrow())
    }

    @Test
    fun `a server error fails the call`() = runTest {
        val repo = repository("", HttpStatusCode.InternalServerError)

        assertTrue(repo.getTodayVerse().isFailure)
    }

    @Test
    fun `weekly verse maps reference text and translation`() = runTest {
        val body = """
            {"success":true,"data":{
              "reference":{"ko":"시편 23:1","en":"Psalm 23:1","de":"Psalm 23,1"},
              "text":"여호와는 나의 목자시니 내게 부족함이 없으리로다",
              "translation":"개역개정"}}
        """.trimIndent()

        val verse = repository(body).getWeeklyVerse().getOrThrow()

        assertEquals("시편 23:1", verse?.reference)
        assertEquals("여호와는 나의 목자시니 내게 부족함이 없으리로다", verse?.text)
        assertEquals("개역개정", verse?.translation)
    }

    @Test
    fun `a weekly verse without text is dropped`() = runTest {
        val body = """{"success":true,"data":{"reference":{"ko":"시편 23:1"},"text":"  "}}"""

        assertNull(repository(body).getWeeklyVerse().getOrThrow())
    }

    @Test
    fun `a weekly verse without a reference is dropped`() = runTest {
        val body = """{"success":true,"data":{"text":"여호와는 나의 목자시니"}}"""

        assertNull(repository(body).getWeeklyVerse().getOrThrow())
    }

    @Test
    fun `no weekly verse set means an empty card`() = runTest {
        val body = """{"success":true,"data":null}"""

        assertNull(repository(body).getWeeklyVerse().getOrThrow())
    }

    @Test
    fun `weekly verse carries the week it belongs to`() = runTest {
        // Not necessarily the running week: with nothing published the server answers
        // with the most recent verse, and this span is what says so on the card.
        val body = """
            {"success":true,"data":{
              "reference":{"ko":"신명기 1:33"},
              "text":"그는 너희보다 먼저 그 길을 가시며",
              "weekStart":"2026-08-30","weekEnd":"2026-09-05"}}
        """.trimIndent()

        val verse = repository(body).getWeeklyVerse().getOrThrow()

        assertEquals(LocalDate(2026, 8, 30), verse?.weekStart)
        assertEquals(LocalDate(2026, 9, 5), verse?.weekEnd)
    }

    @Test
    fun `a span the server cannot express does not cost the card its verse`() = runTest {
        val body = """
            {"success":true,"data":{
              "reference":{"ko":"시편 23:1"},"text":"여호와는 나의 목자시니","weekStart":"nonsense"}}
        """.trimIndent()

        val verse = repository(body).getWeeklyVerse().getOrThrow()

        assertEquals("시편 23:1", verse?.reference)
        assertNull(verse?.weekStart)
    }

    @Test
    fun `a loaded weekly verse is remembered for the next empty answer`() = runTest {
        val memory = FakeVersePreferences()
        val body = """
            {"success":true,"data":{
              "reference":{"ko":"신명기 1:33"},
              "text":"그는 너희보다 먼저 그 길을 가시며",
              "weekStart":"2026-08-30","weekEnd":"2026-09-05"}}
        """.trimIndent()

        repository(body, preferences = memory).getWeeklyVerse().getOrThrow()

        assertEquals(
            RememberedWeeklyVerse("신명기 1:33", LocalDate(2026, 8, 30), LocalDate(2026, 9, 5)),
            memory.rememberedWeekly(),
        )
    }

    @Test
    fun `an empty answer leaves the remembered verse standing`() = runTest {
        // The card falls back to this. Clearing it on an empty week would take away
        // the only thing left to show, in exactly the case it exists for.
        val kept = RememberedWeeklyVerse("시편 23:1", LocalDate(2026, 8, 23), LocalDate(2026, 8, 29))
        val memory = FakeVersePreferences(kept)
        val repo = repository("""{"success":true,"data":null}""", preferences = memory)

        assertNull(repo.getWeeklyVerse().getOrThrow())
        assertEquals(kept, memory.rememberedWeekly())
        assertEquals(0, memory.writes)
    }

    @Test
    fun `an unreachable bible source fails rather than claiming no verse`() = runTest {
        // The server answers 503 when the church's bible API cannot be reached and
        // distinguishes that from "nothing set". Flattening it to null would tell the
        // member there is no verse this week when there is one.
        val repo = repository("", HttpStatusCode.ServiceUnavailable)

        assertTrue(repo.getWeeklyVerse().isFailure)
    }
}
