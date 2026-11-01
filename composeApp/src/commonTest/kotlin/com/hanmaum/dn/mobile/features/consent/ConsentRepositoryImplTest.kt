package com.hanmaum.dn.mobile.features.consent

import com.hanmaum.dn.mobile.features.consent.data.repository.ConsentRepositoryImpl
import com.hanmaum.dn.mobile.features.consent.data.repository.privacyPolicyLink
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentException
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentProblem
import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentPurpose
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ConsentRepositoryImplTest {
    private fun client(status: HttpStatusCode, response: String, capture: (io.ktor.client.request.HttpRequestData) -> Unit = {}) =
        HttpClient(MockEngine { request ->
            capture(request)
            respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            defaultRequest { url("https://backend.example/api/v1/") }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    @Test fun policiesUseThePublicEndpointAndPreserveVersions() = runTest {
        val c = client(HttpStatusCode.OK,
            """{"success":true,"data":[{"purpose":"APP_PRIVACY","version":"v17","requiredForAccount":true}]}""") {
            assertEquals("/api/v1/consents/policies", it.url.encodedPath)
        }
        assertEquals("v17", ConsentRepositoryImpl(c).policies().getOrThrow().single().version)
        c.close()
    }

    @Test fun grantSendsTheNestedAcceptedVersionAndValidatesTheProof() = runTest {
        val c = client(HttpStatusCode.Created,
            """{"success":true,"data":{"purpose":"APP_PRIVACY","state":"GRANTED","requiredForAccount":true,"currentVersion":"v17","grantedVersion":"v17","grantedAt":"2026-10-24T12:00:00Z"}}""") {
            assertEquals(HttpMethod.Post, it.method)
            assertEquals("/api/v1/members/me/consents", it.url.encodedPath)
            val body = Json.parseToJsonElement((it.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
            assertEquals("APP_PRIVACY", body.getValue("purpose").jsonPrimitive.content)
            val consent = body.getValue("consent").jsonObject
            assertTrue(consent.getValue("accepted").jsonPrimitive.boolean)
            assertEquals("v17", consent.getValue("version").jsonPrimitive.content)
        }
        assertTrue(ConsentRepositoryImpl(c).grant(ConsentPurpose.APP_PRIVACY, "v17").getOrThrow().isCurrent)
        c.close()
    }

    @Test fun unknownPurposesAndStatesFromANewerServerAreSkipped() = runTest {
        val policies = client(HttpStatusCode.OK,
            """{"success":true,"data":[{"purpose":"APP_PRIVACY","version":"v1","requiredForAccount":true},{"purpose":"NEWSLETTER","version":"v9","requiredForAccount":false}]}""")
        assertEquals(listOf(ConsentPurpose.APP_PRIVACY),
            ConsentRepositoryImpl(policies).policies().getOrThrow().map { it.purpose })
        policies.close()
        val mine = client(HttpStatusCode.OK,
            """{"success":true,"data":{"actionRequired":false,"consents":[{"purpose":"APP_PRIVACY","state":"GRANTED","requiredForAccount":true,"currentVersion":"v1","grantedVersion":"v1"},{"purpose":"NEWSLETTER","state":"GRANTED","requiredForAccount":false,"currentVersion":"v9","grantedVersion":"v9"},{"purpose":"COURSE_APPLICATION","state":"PAUSED","requiredForAccount":false,"currentVersion":"v2"}]}}""")
        val result = ConsentRepositoryImpl(mine).myConsents().getOrThrow()
        assertEquals(listOf(ConsentPurpose.APP_PRIVACY), result.consents.map { it.purpose })
        assertTrue(result.allowsAccess)
        mine.close()
    }

    @Test fun grantedAtIsParsedToAnInstantAndMalformedStaysUnknown() = runTest {
        fun body(grantedAt: String) = """{"success":true,"data":{"actionRequired":false,"consents":[{"purpose":"APP_PRIVACY","state":"GRANTED","requiredForAccount":true,"currentVersion":"v1","grantedVersion":"v1","grantedAt":"$grantedAt"}]}}"""
        val good = client(HttpStatusCode.OK, body("2026-10-24T12:00:00Z"))
        assertEquals(Instant.parse("2026-10-24T12:00:00Z"), ConsentRepositoryImpl(good).myConsents().getOrThrow().privacy?.grantedAt)
        good.close()
        val bad = client(HttpStatusCode.OK, body("yesterday"))
        assertEquals(null, ConsentRepositoryImpl(bad).myConsents().getOrThrow().privacy?.grantedAt)
        bad.close()
    }

    @Test fun conflictIsAConsentVersionError() = runTest {
        val c = client(HttpStatusCode.Conflict, """{"code":"CONSENT_VERSION_OUTDATED","message":"changed"}""")
        val error = ConsentRepositoryImpl(c).grant(ConsentPurpose.APP_PRIVACY, "v1").exceptionOrNull()
        assertEquals(ConsentProblem.VERSION_OUTDATED, assertIs<ConsentException>(error).problem)
        c.close()
    }

    @Test fun aMissingOrUnknownProofIsNotSuccessfulAccess() = runTest {
        for (body in listOf(
            """{"success":true,"data":null}""",
            """{"success":true,"data":{"actionRequired":false,"consents":[]}}""",
            """{"success":true,"data":{"actionRequired":false,"consents":[{"purpose":"APP_PRIVACY","state":"UNRECOGNIZED","requiredForAccount":true,"currentVersion":"v1"}]}}""",
        )) {
            val c = client(HttpStatusCode.OK, body)
            assertTrue(ConsentRepositoryImpl(c).myConsents().isFailure)
            c.close()
        }
    }

    @Test fun withdrawalUsesDeleteAndRequires204() = runTest {
        val c = client(HttpStatusCode.NoContent, "") {
            assertEquals(HttpMethod.Delete, it.method)
            assertEquals("/api/v1/members/me/consents/APP_PRIVACY", it.url.encodedPath)
        }
        assertTrue(ConsentRepositoryImpl(c).withdraw(ConsentPurpose.APP_PRIVACY).isSuccess)
        c.close()
    }

    @Test fun cancellationIsNotReportedAsABusinessFailure() = runTest {
        val c = HttpClient(MockEngine { throw CancellationException("cancelled") })
        assertFailsWith<CancellationException> { ConsentRepositoryImpl(c).policies() }
        c.close()
    }

    @Test fun policyLinksMustBePublishedHttpsUrls() {
        for (value in listOf("", "http://church.example/privacy", "javascript:alert(1)", "https://user:pass@church.example/policy")) {
            assertFalse(privacyPolicyLink(value).isAvailable)
        }
        assertEquals("https://church.example/privacy", privacyPolicyLink("https://church.example/privacy").url)
    }
}
