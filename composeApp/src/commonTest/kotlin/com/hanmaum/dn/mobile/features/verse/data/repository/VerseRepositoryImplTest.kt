package com.hanmaum.dn.mobile.features.verse.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
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

class VerseRepositoryImplTest {

    @Test
    fun `maps reference translation and link`() = runTest {
        val body = """
            {"success":true,"data":{
              "reference":{"ko":"신명기 3:1-11","en":"Deuteronomy 3:1-11","de":"5. Mose 3,1-11"},
              "translation":"개역개정",
              "sourceUrl":"https://bible.asher.design/quiettime.php?qt_date=2026-09-08"}}
        """.trimIndent()

        val verse = VerseRepositoryImpl(mockClient(body)).getTodayVerse().getOrThrow()

        assertEquals("신명기 3:1-11", verse?.referenceKo)
        assertEquals("Deuteronomy 3:1-11", verse?.referenceEn)
        assertEquals("개역개정", verse?.translation)
        assertEquals("https://bible.asher.design/quiettime.php?qt_date=2026-09-08", verse?.sourceUrl)
    }

    @Test
    fun `trims padding the upstream leaves on the fields`() = runTest {
        val body = """
            {"success":true,"data":{
              "reference":{"ko":"  신명기 3:1-11  ","en":"  Deuteronomy 3:1-11 "},
              "translation":" 개역개정 ","sourceUrl":" https://example.org "}}
        """.trimIndent()

        val verse = VerseRepositoryImpl(mockClient(body)).getTodayVerse().getOrThrow()

        assertEquals("신명기 3:1-11", verse?.referenceKo)
        assertEquals("Deuteronomy 3:1-11", verse?.referenceEn)
        assertEquals("개역개정", verse?.translation)
        assertEquals("https://example.org", verse?.sourceUrl)
    }

    @Test
    fun `one language alone still carries the card`() = runTest {
        val body = """{"success":true,"data":{"reference":{"ko":"시편 23:1"}}}"""

        val verse = VerseRepositoryImpl(mockClient(body)).getTodayVerse().getOrThrow()

        assertEquals("시편 23:1", verse?.referenceKo)
        assertEquals("", verse?.referenceEn)
        assertNull(verse?.sourceUrl)
    }

    @Test
    fun `a row without any reference is dropped`() = runTest {
        val body = """{"success":true,"data":{"reference":{"ko":"","en":" "},"translation":"개역개정"}}"""

        assertNull(VerseRepositoryImpl(mockClient(body)).getTodayVerse().getOrThrow())
    }

    @Test
    fun `an empty envelope means no passage today`() = runTest {
        val body = """{"success":true,"data":null}"""

        assertNull(VerseRepositoryImpl(mockClient(body)).getTodayVerse().getOrThrow())
    }

    @Test
    fun `no content means no passage today`() = runTest {
        val repo = VerseRepositoryImpl(mockClient("", HttpStatusCode.NoContent))

        assertNull(repo.getTodayVerse().getOrThrow())
    }

    @Test
    fun `not found means no passage today`() = runTest {
        val repo = VerseRepositoryImpl(mockClient("", HttpStatusCode.NotFound))

        assertNull(repo.getTodayVerse().getOrThrow())
    }

    @Test
    fun `a server error fails the call`() = runTest {
        val repo = VerseRepositoryImpl(mockClient("", HttpStatusCode.InternalServerError))

        assertTrue(repo.getTodayVerse().isFailure)
    }
}
