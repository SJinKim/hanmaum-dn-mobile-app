package com.hanmaum.dn.mobile.features.geofence.data.repository

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    responseJson: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    onRequest: ((HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = status,
        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
    )
}) {
    install(ContentNegotiation) { json(testJson) }
    defaultRequest { url("http://localhost") }
}

class ChurchLocationRepositoryImplTest {

    private val validJson = """{"latitude":37.1234,"longitude":127.5678,"radiusMeters":100.0}"""

    @Test
    fun getChurchLocation_returnsMappedLocation() = runTest {
        val repo = ChurchLocationRepositoryImpl(mockClient(validJson))
        val result = repo.getChurchLocation()

        assertTrue(result.isSuccess)
        assertEquals(ChurchLocation(37.1234, 127.5678, 100.0), result.getOrThrow())
    }

    @Test
    fun getChurchLocation_returnsFailureOnServerError() = runTest {
        val repo = ChurchLocationRepositoryImpl(mockClient("{}", HttpStatusCode.InternalServerError))
        val result = repo.getChurchLocation()

        assertTrue(result.isFailure)
    }

    @Test
    fun getChurchLocation_requestsCorrectPath() = runTest {
        var capturedPath = ""
        val repo = ChurchLocationRepositoryImpl(
            mockClient(validJson) { request -> capturedPath = request.url.encodedPath }
        )

        repo.getChurchLocation()
        assertEquals("/church/location", capturedPath)
    }
}
