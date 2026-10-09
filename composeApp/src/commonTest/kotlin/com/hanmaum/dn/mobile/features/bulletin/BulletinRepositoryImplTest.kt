package com.hanmaum.dn.mobile.features.bulletin

import com.hanmaum.dn.mobile.core.network.createHttpClient
import com.hanmaum.dn.mobile.features.bulletin.data.repository.BulletinRepositoryImpl
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import com.hanmaum.dn.mobile.features.bulletin.domain.model.BulletinSectionKey
import kotlin.test.*
import kotlin.time.Instant

class BulletinRepositoryImplTest {
    private var body = """{"success":true,"data":$bulletinJson}"""
    private var status = HttpStatusCode.OK
    private var failure: Exception? = null
    private var time = Instant.parse("2026-10-10T11:00:00Z")
    private val settings = MapSettings()
    private val tokens = BulletinTokens()
    private val requests = mutableListOf<String>()
    private val engine = MockEngine { request ->
        requests += request.url.toString()
        failure?.let { throw it }
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }
    private val client = createHttpClient(tokens, engine)
    private fun repo() = BulletinRepositoryImpl(client, settings, tokens) { time }
    @AfterTest fun close() { client.close() }

    @Test fun `current contract maps ordered content and server settings`() = runTest {
        val read = repo().getCurrent().getOrThrow()!!
        assertNull(read.cachedAt)
        val edition = read.bulletin
        assertEquals(LocalDate(2026, 10, 11), edition.serviceDate)
        assertEquals(41, edition.volume)
        assertEquals("3부 예배", edition.serviceName)
        assertEquals(LocalTime(14, 0), edition.serviceStartTime)
        assertEquals("Custom worship title", edition.sectionTitle(BulletinSectionKey.SECTION_WORSHIP))
        assertEquals(listOf("First song", "Second song"), edition.songs)
        assertEquals(listOf("First notice", "Second notice"), edition.announcements.map { it.title })
        assertEquals("John 21:16", edition.sharingBlocks[2].reference)
        assertTrue(requests.single().endsWith("/api/v1/bulletins/current"))
    }

    @Test fun `missing current is an empty result and invalidates the cached current`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        status = HttpStatusCode.NotFound
        assertNull(repository.getCurrent().getOrThrow())
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repository.getCurrent().isFailure)
    }

    @Test fun `transport failure returns a dated persisted copy across repository instances`() = runTest {
        repo().getCurrent().getOrThrow()
        failure = kotlinx.io.IOException("offline")
        val read = repo().getCurrent().getOrThrow()!!
        assertEquals(time, read.cachedAt)
        assertEquals("Begin again", read.bulletin.sermonTitle)
    }

    @Test fun `server outage without cache is a failure`() = runTest {
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repo().getCurrent().isFailure)
    }

    @Test fun `a 5xx outage with cache keeps the published copy`() = runTest {
        repo().getCurrent().getOrThrow()
        status = HttpStatusCode.InternalServerError
        assertNotNull(repo().getCurrent().getOrThrow()?.cachedAt)
    }

    @Test fun `fresh content replaces the old cached version`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        body = body.replace("Begin again", "Updated sermon")
        assertEquals("Updated sermon", repository.getCurrent().getOrThrow()?.bulletin?.sermonTitle)
        status = HttpStatusCode.ServiceUnavailable
        assertEquals("Updated sermon", repository.getCurrent().getOrThrow()?.bulletin?.sermonTitle)
    }

    @Test fun `a copy older than 24 hours is not used`() = runTest {
        repo().getCurrent().getOrThrow()
        time = Instant.fromEpochSeconds(time.epochSeconds + 24 * 60 * 60 + 1)
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repo().getCurrent().isFailure)
    }

    @Test fun `cache cannot leak between signed in members`() = runTest {
        repo().getCurrent().getOrThrow()
        tokens.subject = "member-2"
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repo().getCurrent().isFailure)
        tokens.clear()
        assertTrue(repo().getCurrent().isFailure)
    }

    @Test fun `permission denial removes the cached content before a later outage`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        status = HttpStatusCode.Forbidden
        assertTrue(repository.getCurrent().isFailure)
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repository.getCurrent().isFailure)
    }

    @Test fun `unauthorized does not show cached content`() = runTest {
        repo().getCurrent().getOrThrow()
        status = HttpStatusCode.Unauthorized
        assertTrue(repo().getCurrent().isFailure)
    }

    @Test fun `draft withdrawn and unknown states are rejected`() = runTest {
        listOf("DRAFT", "WITHDRAWN", "UNKNOWN").forEach { state ->
            body = """{"success":true,"data":${bulletinJson.replace("PUBLISHED", state)}}"""
            assertNull(repo().getCurrent().getOrThrow())
        }
        body = """{"success":true,"data":${bulletinJson.replace("\"withdrawnAt\":null", "\"withdrawnAt\":\"2026-10-10T12:00:00Z\"")}}"""
        assertNull(repo().getCurrent().getOrThrow())
    }

    @Test fun `invalid 200 response does not become no published bulletin or fallback`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        listOf("""{"success":false,"data":null}""", """{"success":true,"data":null}""", "invalid json").forEach {
            body = it
            assertTrue(repository.getCurrent().isFailure)
        }
    }

    @Test fun `by date uses query and does not replace current selection in cache`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        body = body.replace("edition-1", "edition-older").replace("2026-10-11", "2026-10-04")
        assertEquals(LocalDate(2026, 10, 4), repository.getByDate(LocalDate(2026, 10, 4)).getOrThrow()?.bulletin?.serviceDate)
        assertTrue(requests.last().contains("/api/v1/bulletins?date=2026-10-04"))
        status = HttpStatusCode.ServiceUnavailable
        assertEquals(LocalDate(2026, 10, 11), repository.getCurrent().getOrThrow()?.bulletin?.serviceDate)
        assertEquals(LocalDate(2026, 10, 4), repository.getByDate(LocalDate(2026, 10, 4)).getOrThrow()?.bulletin?.serviceDate)
    }

    @Test fun `withdrawn historic edition is removed without deleting another current edition`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        body = body.replace("edition-1", "edition-older").replace("2026-10-11", "2026-10-04")
        repository.getByDate(LocalDate(2026, 10, 4)).getOrThrow()
        status = HttpStatusCode.NotFound
        assertNull(repository.getByDate(LocalDate(2026, 10, 4)).getOrThrow())
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repository.getByDate(LocalDate(2026, 10, 4)).isFailure)
        assertNotNull(repository.getCurrent().getOrThrow())
    }

    @Test fun `date mismatch is a failure`() = runTest {
        assertTrue(repo().getByDate(LocalDate(2026, 10, 4)).isFailure)
    }

    @Test fun `history keeps server order filters unpublished rows and respects pagination`() = runTest {
        body = """{"success":true,"data":{"content":[
          {"publicId":"one","serviceDate":"2026-10-11","volume":41,"status":"PUBLISHED","sermonTitle":"First"},
          {"publicId":"draft","serviceDate":"2026-10-04","status":"DRAFT"},
          {"publicId":"two","serviceDate":"2026-09-27","status":"PUBLISHED"}],"last":false}}"""
        val page = repo().getHistory(2).getOrThrow()
        assertEquals(listOf("one", "two"), page.editions.map { it.publicId })
        assertTrue(page.hasNext)
        assertTrue(requests.last().contains("page=2"))
        assertTrue(requests.last().contains("size=20"))
    }

    @Test fun `history permission denial also invalidates the detail cache`() = runTest {
        repo().getCurrent().getOrThrow()
        status = HttpStatusCode.Forbidden
        assertTrue(repo().getHistory(0).isFailure)
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repo().getCurrent().isFailure)
    }

    @Test fun `Sunday opens at Friday midnight in Berlin rather than UTC`() = runTest {
        time = Instant.parse("2026-10-08T21:59:59Z")
        assertNull(repo().getCurrent().getOrThrow())
        time = Instant.parse("2026-10-08T22:00:00Z")
        assertEquals(LocalDate(2026, 10, 11), repo().getCurrent().getOrThrow()?.bulletin?.serviceDate)
    }

    @Test fun `a published later Sunday from an old server is not shown or cached`() = runTest {
        body = body.replace("2026-10-11", "2026-10-18")
        assertNull(repo().getCurrent().getOrThrow())
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repo().getCurrent().isFailure)
    }

    @Test fun `future current from an old server preserves a valid older offline copy`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        body = body.replace("2026-10-11", "2026-10-18")
        assertNull(repository.getCurrent().getOrThrow())
        status = HttpStatusCode.ServiceUnavailable
        val read = repository.getCurrent().getOrThrow()!!
        assertEquals(LocalDate(2026, 10, 11), read.bulletin.serviceDate)
        assertNotNull(read.cachedAt)
    }

    @Test fun `all history entries use one visibility cutoff across Friday midnight`() = runTest {
        body = """{"success":true,"data":{"content":[
          {"publicId":"one","serviceDate":"2026-10-11","status":"PUBLISHED"},
          {"publicId":"two","serviceDate":"2026-10-11","status":"PUBLISHED"}],"last":true}}"""
        var clockCalls = 0
        val repository = BulletinRepositoryImpl(client, settings, tokens) {
            clockCalls++
            Instant.parse(if (clockCalls == 1) "2026-10-08T21:59:59Z" else "2026-10-08T22:00:00Z")
        }
        assertTrue(repository.getHistory(0).getOrThrow().editions.isEmpty())
        assertEquals(1, clockCalls)
    }

    @Test fun `a history page of only future editions retains its next page flag`() = runTest {
        body = """{"success":true,"data":{"content":[
          {"publicId":"future","serviceDate":"2026-10-18","status":"PUBLISHED"}],"last":false}}"""
        val page = repo().getHistory(0).getOrThrow()
        assertTrue(page.editions.isEmpty())
        assertTrue(page.hasNext)
    }

    @Test fun `a future date cannot be requested even while offline`() = runTest {
        failure = kotlinx.io.IOException("offline")
        assertNull(repo().getByDate(LocalDate(2026, 10, 18)).getOrThrow())
        assertTrue(requests.isEmpty())
    }

    @Test fun `legacy future cache is not replayed after upgrading the app`() = runTest {
        repo().getCurrent().getOrThrow()
        val cache = settings.getString("bulletin_cache_v1", "")
        assertTrue(cache.contains("2026-10-11"))
        settings.putString("bulletin_cache_v1", cache.replace("2026-10-11", "2026-10-18"))
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repo().getCurrent().isFailure)
        assertNull(repo().getByDate(LocalDate(2026, 10, 18)).getOrThrow())
    }

    @Test fun `history excludes later Sundays and preserves older editions and pagination`() = runTest {
        time = Instant.parse("2026-10-09T08:00:00Z")
        body = """{"success":true,"data":{"content":[
          {"publicId":"future","serviceDate":"2026-10-18","status":"PUBLISHED"},
          {"publicId":"this-week","serviceDate":"2026-10-11","status":"PUBLISHED"},
          {"publicId":"past","serviceDate":"2026-10-04","status":"PUBLISHED"}],"last":false}}"""
        val repository = repo()
        val friday = repository.getHistory(0).getOrThrow()
        assertEquals(listOf("this-week", "past"), friday.editions.map { it.publicId })
        assertTrue(friday.hasNext)
        time = Instant.parse("2026-10-08T08:00:00Z")
        assertEquals(listOf("past"), repository.getHistory(0).getOrThrow().editions.map { it.publicId })
    }

    @Test fun `cancellation propagates instead of rendering offline content`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        failure = CancellationException("cancelled")
        assertFailsWith<CancellationException> { repository.getCurrent() }
    }

    @Test fun `observed withdrawal is not replayed from older cache on the next outage`() = runTest {
        val repository = repo()
        repository.getCurrent().getOrThrow()
        body = body.replace("PUBLISHED", "WITHDRAWN")
        assertNull(repository.getCurrent().getOrThrow())
        status = HttpStatusCode.ServiceUnavailable
        assertTrue(repository.getCurrent().isFailure)
    }

    @Test fun `future block types are skipped while known content stays ordered`() = runTest {
        body = body.replace("\"HEADING\"", "\"FUTURE_BLOCK\"")
        val edition = repo().getCurrent().getOrThrow()!!.bulletin
        assertEquals(listOf("Paragraph", "Scripture text", "First question"), edition.sharingBlocks.map { it.text })
    }

    @Test fun `blank section title uses the server default and future keys are ignored`() = runTest {
        body = body.replace("Custom worship title", "")
            .replace("\"SECTION_OFFERING\"", "\"FUTURE_SECTION\"")
        val edition = repo().getCurrent().getOrThrow()!!.bulletin
        assertEquals("경배와 찬양", edition.sectionTitle(BulletinSectionKey.SECTION_WORSHIP))
        assertFalse(edition.sectionTitles.containsKey(BulletinSectionKey.UNKNOWN))
    }
}
