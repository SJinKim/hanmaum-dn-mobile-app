package com.hanmaum.dn.mobile.features.login.data.repository

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val testJson = Json { ignoreUnknownKeys = true }

// Real OpenPLZ API response shape (extra fields included to prove ignoreUnknownKeys).
private const val BERLIN_RESPONSE =
    """[{"postalCode":"10115","name":"Berlin","municipality":{"key":"11000000","name":"Berlin, Stadt","type":"Kreisfreie Stadt"},"federalState":{"key":"11","name":"Berlin"}}]"""

private fun mockClient(
    responseJson: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    onRequest: ((HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
}

class CityLookupRepositoryImplTest {

    @Test
    fun cityForPostalCode_returnsFirstLocalityName() = runTest {
        val client = mockClient(BERLIN_RESPONSE)
        val result = CityLookupRepositoryImpl(client).cityForPostalCode("10115")

        assertEquals("Berlin", result)
    }

    @Test
    fun cityForPostalCode_returnsNullWhenNoMatch() = runTest {
        val client = mockClient("[]")
        val result = CityLookupRepositoryImpl(client).cityForPostalCode("00000")

        assertNull(result)
    }

    @Test
    fun cityForPostalCode_returnsNullOnServerError() = runTest {
        val client = mockClient("", status = HttpStatusCode.InternalServerError)
        val result = CityLookupRepositoryImpl(client).cityForPostalCode("10115")

        assertNull(result)
    }

    @Test
    fun cityForPostalCode_queriesOpenPlzWithPostalCode() = runTest {
        var host = ""
        var path = ""
        var postalCode: String? = null
        val client = mockClient(BERLIN_RESPONSE) { request ->
            host = request.url.host
            path = request.url.encodedPath
            postalCode = request.url.parameters["postalCode"]
        }
        CityLookupRepositoryImpl(client).cityForPostalCode("10115")

        assertEquals("openplzapi.org", host)
        assertEquals("/de/Localities", path)
        assertEquals("10115", postalCode)
    }
}
