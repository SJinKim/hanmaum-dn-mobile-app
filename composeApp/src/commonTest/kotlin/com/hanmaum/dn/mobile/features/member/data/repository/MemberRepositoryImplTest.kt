package com.hanmaum.dn.mobile.features.member.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.TextContent
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

class MemberRepositoryImplTest {

    private val member = MemberResponse(
        publicId = "u1",
        firstName = "Seungjin",
        lastName = "Kim",
        status = MemberStatus.ACTIVE,
        street = "Musterstraße",
        houseNumber = "12",
        zipCode = "50667",
        city = "Köln",
    )

    @Test
    fun getMyProfile_mapsHouseNumber() = runTest {
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val result = MemberRepositoryImpl(mockClient(json)).getMyProfile()
        val p = result.getOrThrow()
        assertEquals("Musterstraße", p.street)
        assertEquals("12", p.houseNumber)
    }

    @Test
    fun updateMyProfile_patchesMembersMe() = runTest {
        var method: HttpMethod? = null
        var path = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req ->
            method = req.method
            path = req.url.encodedPath
        }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null,
            street = "Musterstraße", houseNumber = "12", zipCode = "50667", city = "Köln",
        )
        assertEquals(HttpMethod.Patch, method)
        assertEquals("/members/me", path)
    }

    @Test
    fun updateMyProfile_sendsHouseNumberAndCamelCaseZipCode() = runTest {
        // Regression for the Hausnummer bug: the old request DTO had no
        // houseNumber field at all and misnamed zipCode as zip_code (the
        // backend is default-Jackson camelCase, so zip_code was silently
        // dropped server-side).
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null,
            street = "Musterstraße", houseNumber = "12", zipCode = "50667", city = "Köln",
        )
        assertTrue(body.contains("\"houseNumber\":\"12\""), "body was $body")
        assertTrue(body.contains("\"zipCode\":\"50667\""), "body was $body")
        assertFalse(body.contains("zip_code"), "body was $body")
    }

    @Test
    fun updateMyProfile_omitsNullFieldsFromBody() = runTest {
        // Backend PATCH semantics are null-means-keep; kotlinx must OMIT null
        // fields (encodeDefaults=false), not send explicit nulls.
        var body = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = member))
        val client = mockClient(json) { req -> body = (req.body as TextContent).text }
        MemberRepositoryImpl(client).updateMyProfile(
            phoneNumber = null, profileImageUrl = null,
            street = null, houseNumber = null, zipCode = null, city = "Köln",
        )
        assertFalse(body.contains("houseNumber"), "body was $body")
        assertFalse(body.contains("street"), "body was $body")
        assertTrue(body.contains("\"city\":\"Köln\""), "body was $body")
    }
}
