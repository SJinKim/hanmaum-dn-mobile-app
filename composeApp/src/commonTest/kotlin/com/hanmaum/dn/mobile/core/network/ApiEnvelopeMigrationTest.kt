package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.features.album.data.repository.AlbumsRepositoryImpl
import com.hanmaum.dn.mobile.features.album.domain.model.Album
import com.hanmaum.dn.mobile.features.floorplan.data.repository.FloorPlanRepositoryImpl
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Point
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import com.hanmaum.dn.mobile.features.geofence.data.repository.ChurchLocationRepositoryImpl
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApiEnvelopeMigrationTest {
    private data class Endpoint(val path: String, val payload: String, val expected: Any)

    private val endpoints = listOf(
        Endpoint("albums", """[{"publicId":"a2","name":"Second","pcloudCode":"c2","displayOrder":2},{"publicId":"a1","name":"First","pcloudCode":"c1","displayOrder":1}]""",
            listOf(Album("a1", "First", "c1", 1), Album("a2", "Second", "c2", 2))),
        Endpoint("floorplan/floors", """[{"id":"f1","floorNumber":1,"name":"First"}]""",
            listOf(Floor("f1", 1, "First"))),
        Endpoint("floorplan/floors/f1/rooms", """[{"id":"r1","floorId":"f1","name":"Room","description":"Test","points":[[0.1,0.2],[0.3,0.4]]}]""",
            listOf(Room("r1", "f1", "Room", "Test", listOf(Point(0.1f, 0.2f), Point(0.3f, 0.4f))))),
        Endpoint("church/location", """{"latitude":37.1,"longitude":127.2,"radiusMeters":100.0}""",
            ChurchLocation(37.1, 127.2, 100.0)),
    )

    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK, cancel: Boolean = false) =
        HttpClient(MockEngine {
            if (cancel) throw CancellationException("cancelled request")
            respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
            defaultRequest { url("https://backend.test/") }
        }

    private suspend fun read(endpoint: Endpoint, client: HttpClient): Result<Any> = when (endpoint.path) {
        "albums" -> AlbumsRepositoryImpl(client).getAlbums()
        "floorplan/floors" -> Result.success(FloorPlanRepositoryImpl(client).getFloors())
        "floorplan/floors/f1/rooms" -> Result.success(FloorPlanRepositoryImpl(client).getRooms("f1"))
        else -> ChurchLocationRepositoryImpl(client).getChurchLocation()
    }

    private suspend fun checkBody(endpoint: Endpoint, body: String, status: HttpStatusCode = HttpStatusCode.OK): Result<Any> {
        val client = client(body, status)
        return try { read(endpoint, client) } finally { client.close() }
    }

    private fun assertFailure(endpoint: Endpoint, result: Result<Any>) {
        if (endpoint.path.startsWith("floorplan/")) assertEquals(emptyList<Any>(), result.getOrThrow(), endpoint.path)
        else assertTrue(result.isFailure, endpoint.path)
    }

    @Test
    fun legacyAndWrappedResponsesHaveIdenticalDomainData() = runTest {
        for (endpoint in endpoints) {
            assertEquals(endpoint.expected, checkBody(endpoint, endpoint.payload).getOrThrow(), endpoint.path)
            val wrapped = """{"success":true,"message":"ok","extra":"ignored","data":${endpoint.payload}}"""
            assertEquals(endpoint.expected, checkBody(endpoint, wrapped).getOrThrow(), endpoint.path)
        }
    }

    @Test
    fun emptyListsWorkInBothFormats() = runTest {
        for (endpoint in endpoints.filter { it.path != "church/location" }) {
            for (body in listOf("[]", """{"success":true,"data":[]}""")) {
                assertEquals(emptyList<Any>(), checkBody(endpoint, body).getOrThrow(), endpoint.path)
            }
        }
    }

    @Test
    fun unsuccessfulEnvelopesDoNotExposeTheirData() = runTest {
        for (endpoint in endpoints) {
            assertFailure(endpoint, checkBody(endpoint, """{"success":false,"message":"failed","data":${endpoint.payload}}"""))
        }
    }

    @Test
    fun missingNullAndMalformedDataAreRejected() = runTest {
        for (endpoint in endpoints) {
            for (body in listOf("""{"success":true}""", """{"success":true,"data":null}""",
                """{"success":true,"data":"wrong shape"}""", "not json")) {
                assertFailure(endpoint, checkBody(endpoint, body))
            }
        }
    }

    @Test
    fun unsuccessfulHttpStatusNeverReturnsValidLookingData() = runTest {
        for (endpoint in endpoints) {
            assertFailure(endpoint, checkBody(endpoint, endpoint.payload, HttpStatusCode.ServiceUnavailable))
        }
    }

    @Test
    fun coroutineCancellationIsPropagatedByEveryRepository() = runTest {
        for (endpoint in endpoints) {
            val client = client(endpoint.payload, cancel = true)
            try {
                assertFailsWith<CancellationException>(endpoint.path) { read(endpoint, client) }
            } finally { client.close() }
        }
    }
}
