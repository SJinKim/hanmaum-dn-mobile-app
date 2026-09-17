package com.hanmaum.dn.mobile.features.ministry.data.repository

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
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
    onRequest: ((io.ktor.client.request.HttpRequestData) -> Unit)? = null,
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

/**
 * The wire name is isActive, pinned server-side by an explicit
 * @get:JsonProperty in hanmaum-dn-server#138 after springdoc had documented
 * it as "active" while Jackson served "isActive". The mobile DTOs read
 * @SerialName("active") — correctly derived from a spec that was wrong —
 * with a default of true, so a deactivated 사역 arrived as active and nothing
 * failed: ignoreUnknownKeys dropped the real key and the default filled the
 * gap. The list view hid it because the server filters on ?active=true; the
 * detail endpoint filters nothing, so that is where users would have seen it.
 */
class MinistryRepositoryImplTest {

    @Test
    fun getMinistriesBindsIsActiveFromTheWire() = runTest {
        val json = """
            {"success":true,"data":[
              {"publicId":"m1","title":"난민 사역","subtitle":"돕는 손길",
               "contacts":[{"name":"김승진","role":"리더"}],"isActive":false}
            ]}
        """.trimIndent()

        val ministries = MinistryRepositoryImpl(mockClient(json)).getMinistries(activeOnly = false).getOrThrow()

        assertEquals(1, ministries.size)
        assertFalse(ministries.single().isActive, "isActive must come from the wire, not from a default")
    }

    @Test
    fun getMinistryDetailBindsIsActiveFromTheWire() = runTest {
        val json = """
            {"success":true,"data":
              {"publicId":"m1","title":"난민 사역","subtitle":"돕는 손길","about":"매주 토요일",
               "contacts":[{"name":"김승진","role":"리더"}],"requirements":[],"isActive":false}
            }
        """.trimIndent()

        val detail = MinistryRepositoryImpl(mockClient(json)).getMinistryDetail("m1").getOrThrow()

        assertFalse(detail.isActive, "a deactivated ministry must not read as active on the detail screen")
    }

    @Test
    fun getMinistriesFailsWhenIsActiveIsMissing() = runTest {
        // No default on the DTO: a future rename of the wire key must break
        // loudly here instead of silently resolving to true.
        val json = """
            {"success":true,"data":[
              {"publicId":"m1","title":"난민 사역","subtitle":"돕는 손길","contacts":[],"active":true}
            ]}
        """.trimIndent()

        val result = MinistryRepositoryImpl(mockClient(json)).getMinistries(activeOnly = false)

        assertTrue(result.isFailure, "a missing isActive must fail, not default to true")
    }

    @Test
    fun getMinistryDetailFailsWhenIsActiveIsMissing() = runTest {
        val json = """
            {"success":true,"data":
              {"publicId":"m1","title":"난민 사역","subtitle":"돕는 손길","contacts":[],"requirements":[],"active":true}
            }
        """.trimIndent()

        val result = MinistryRepositoryImpl(mockClient(json)).getMinistryDetail("m1")

        assertTrue(result.isFailure, "a missing isActive must fail, not default to true")
    }

    // ── the other renamed wire fields (#117) ─────────────────────────────

    @Test
    fun theRenamedFieldsMapOntoTheDomainNames() = runTest {
        // title/subtitle/about are @SerialName renames like isActive was, and
        // only isActive had a test. The same class of mistake fits here.
        val json = """
            {"success":true,"data":
              {"publicId":"m1","title":"난민 사역","subtitle":"돕는 손길","about":"매주 토요일",
               "contacts":[{"name":"김승진","role":"리더"}],"requirements":[],"isActive":true}
            }
        """.trimIndent()
        val d = MinistryRepositoryImpl(mockClient(json)).getMinistryDetail("m1").getOrThrow()
        assertEquals("난민 사역", d.name)
        assertEquals("돕는 손길", d.shortDescription)
        assertEquals("매주 토요일", d.longDescription)
    }

    @Test
    fun theFirstContactBecomesTheLeader() = runTest {
        // The server has no single leader field; the first contact plays that
        // role, and an empty list must not throw.
        val withContacts = """
            {"success":true,"data":[{"publicId":"m1","title":"난민 사역","subtitle":"돕는 손길",
             "contacts":[{"name":"김승진","role":"리더"},{"name":"이서진","role":"부리더"}],"isActive":true}]}
        """.trimIndent()
        val m = MinistryRepositoryImpl(mockClient(withContacts)).getMinistries(activeOnly = true).getOrThrow()
        assertEquals("김승진", m.single().leaderName)

        val without = """
            {"success":true,"data":[{"publicId":"m1","title":"난민 사역","subtitle":"돕는 손길",
             "contacts":[],"isActive":true}]}
        """.trimIndent()
        val n = MinistryRepositoryImpl(mockClient(without)).getMinistries(activeOnly = true).getOrThrow()
        assertEquals(null, n.single().leaderName)
    }

    @Test
    fun theActiveOnlyFlagReachesTheQuery() = runTest {
        var url = ""
        val json = """{"success":true,"data":[]}"""
        MinistryRepositoryImpl(mockClient(json) { url = it.url.toString() }).getMinistries(activeOnly = true)
        assertTrue(url.contains("active=true"), "url was $url")
    }
}
