package com.hanmaum.dn.mobile.features.consent.data.repository

import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentResult
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

private fun mockClient(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    onRequest: (HttpRequestData) -> Unit = {},
): HttpClient = HttpClient(MockEngine { request ->
    onRequest(request)
    respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    // Same settings as the app's client in NetworkClient.kt.
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

/** Response shapes of hanmaum-dn-server's ConsentDtos (hanmaum-dn-server#358). */
class ConsentRepositoryImplTest {

    private val statusJson = """
        {"purpose":"APP_PRIVACY","state":"GRANTED","requiredForAccount":true,"currentVersion":"2026-09",
         "grantedVersion":"2026-09","grantedAt":"2026-09-01T10:00:00Z"}
    """.trimIndent()

    @Test
    fun policiesAreReadAndUnknownPurposesDropped() = runTest {
        var path: String? = null
        val client = mockClient(
            """
            {"success":true,"message":null,"data":[
              {"purpose":"APP_PRIVACY","version":"2026-09","requiredForAccount":true},
              {"purpose":"NEWSLETTER","version":"1","requiredForAccount":false}
            ]}
            """.trimIndent(),
        ) { path = it.url.encodedPath }

        val result = ConsentRepositoryImpl(client).getPolicies()

        assertEquals("/consents/policies", path)
        assertIs<ConsentResult.Success<*>>(result)
        val policies = (result as ConsentResult.Success).data
        assertEquals(1, policies.size)
        assertEquals(ConsentPurpose.APP_PRIVACY, policies.single().purpose)
        assertEquals("2026-09", policies.single().version)
        assertTrue(policies.single().requiredForAccount)
    }

    @Test
    fun myConsentsMapStateAndGrantTime() = runTest {
        val client = mockClient(
            """
            {"success":true,"message":null,"data":{"actionRequired":false,"consents":[$statusJson,
              {"purpose":"COURSE_APPLICATION","state":"SOMETHING_NEW","requiredForAccount":false,
               "currentVersion":"1","grantedVersion":null,"grantedAt":null}]}}
            """.trimIndent(),
        )

        val result = ConsentRepositoryImpl(client).getMyConsents()

        assertIs<ConsentResult.Success<*>>(result)
        val overview = (result as ConsentResult.Success).data
        val privacy = overview.of(ConsentPurpose.APP_PRIVACY)!!
        assertEquals(ConsentState.GRANTED, privacy.state)
        assertEquals(Instant.parse("2026-09-01T10:00:00Z"), privacy.grantedAt)
        // An unknown state must never read as agreement.
        assertEquals(ConsentState.MISSING, overview.of(ConsentPurpose.COURSE_APPLICATION)!!.state)
    }

    @Test
    fun grantSendsThePurposeAndTheVersionShown() = runTest {
        var request: HttpRequestData? = null
        val client = mockClient(
            """{"success":true,"message":null,"data":$statusJson}""",
            status = HttpStatusCode.Created,
        ) { request = it }

        val result = ConsentRepositoryImpl(client).grant(ConsentPurpose.APP_PRIVACY, "2026-09")

        assertIs<ConsentResult.Success<*>>(result)
        assertEquals(HttpMethod.Post, request!!.method)
        assertEquals("/members/me/consents", request!!.url.encodedPath)
        val body = Json.parseToJsonElement((request!!.body as TextContent).text).jsonObject
        assertEquals("APP_PRIVACY", body["purpose"]!!.jsonPrimitive.content)
        val consent = body["consent"]!!.jsonObject
        assertTrue(consent["accepted"]!!.jsonPrimitive.boolean)
        assertEquals("2026-09", consent["version"]!!.jsonPrimitive.content)
    }

    @Test
    fun anOutdatedVersionIsReadFromTheCode() = runTest {
        val client = mockClient(
            """{"status":409,"message":"stale","code":"CONSENT_VERSION_OUTDATED","fieldErrors":null}""",
            status = HttpStatusCode.Conflict,
        )

        assertEquals(ConsentResult.VersionOutdated, ConsentRepositoryImpl(client).grant(ConsentPurpose.APP_PRIVACY, "old"))
    }

    @Test
    fun aMissingConsentIsReadFromTheCode() = runTest {
        val client = mockClient(
            """{"status":400,"message":"required","code":"CONSENT_REQUIRED","fieldErrors":null}""",
            status = HttpStatusCode.BadRequest,
        )

        assertEquals(ConsentResult.ConsentRequired, ConsentRepositoryImpl(client).grant(ConsentPurpose.APP_PRIVACY, "x"))
    }

    @Test
    fun aConflictWithoutTheCodeIsAPlainFailure() = runTest {
        val client = mockClient("", status = HttpStatusCode.Conflict)

        assertEquals(ConsentResult.Failed, ConsentRepositoryImpl(client).grant(ConsentPurpose.APP_PRIVACY, "x"))
    }

    @Test
    fun withdrawDeletesThePurposeAndTakesNoContentAsSuccess() = runTest {
        var request: HttpRequestData? = null
        val client = mockClient("", status = HttpStatusCode.NoContent) { request = it }

        val result = ConsentRepositoryImpl(client).withdraw(ConsentPurpose.APP_PRIVACY)

        assertEquals(ConsentResult.Success(Unit), result)
        assertEquals(HttpMethod.Delete, request!!.method)
        assertEquals("/members/me/consents/APP_PRIVACY", request!!.url.encodedPath)
    }

    @Test
    fun aServerErrorIsAFailure() = runTest {
        val client = mockClient("oops", status = HttpStatusCode.InternalServerError)

        assertEquals(ConsentResult.Failed, ConsentRepositoryImpl(client).getMyConsents())
    }
}
