package com.hanmaum.dn.mobile.features.ministry.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ContactResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistryDetailResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistrySummaryResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ScheduleResponse
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
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

class MinistryRepositoryImplTest {

    private val summary = MinistrySummaryResponse(
        publicId = "m1",
        title = "난민 사역",
        subtitle = "하나님의 사랑을 나누는 사역",
        imageUrl = null,
        contacts = listOf(ContactResponse(role = "팀장", name = "김영원 권사님")),
        active = true,
    )

    private val detail = MinistryDetailResponse(
        publicId = "m1",
        title = "난민 사역",
        subtitle = "하나님의 사랑을 나누는 사역",
        about = "이 사역은...",
        requirements = listOf("큐베세 양육 수료자"),
        schedules = listOf(ScheduleResponse(description = "넷째 주 토요일", startTime = "07:00", endTime = "09:00")),
        contacts = listOf(
            ContactResponse(role = "팀장", name = "김영원 권사님"),
            ContactResponse(role = "간사", name = "최혜령 자매님"),
        ),
        imageUrl = null,
        active = true,
    )

    @Test
    fun getMinistries_mapsSummaryFields() = runTest {
        val json = testJson.encodeToString(ApiResponse(success = true, data = listOf(summary)))
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistries()

        val ministries = result.getOrThrow()
        assertEquals(1, ministries.size)
        val m = ministries[0]
        assertEquals("m1", m.publicId)
        assertEquals("난민 사역", m.title)
        assertEquals("하나님의 사랑을 나누는 사역", m.subtitle)
        assertEquals(true, m.isActive)
        assertEquals(1, m.contacts.size)
        assertEquals("팀장", m.contacts[0].role)
        assertEquals("김영원 권사님", m.contacts[0].name)
    }

    @Test
    fun getMinistries_emptyData_returnsEmptyList() = runTest {
        val json = testJson.encodeToString(
            ApiResponse(success = true, data = emptyList<MinistrySummaryResponse>())
        )
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistries()
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun getMinistries_requestsCorrectPathAndQuery() = runTest {
        var path = ""
        var active = ""
        val json = testJson.encodeToString(
            ApiResponse(success = true, data = emptyList<MinistrySummaryResponse>())
        )
        val client = mockClient(json) { req ->
            path = req.url.encodedPath
            active = req.url.parameters["active"] ?: ""
        }
        MinistryRepositoryImpl(client).getMinistries(activeOnly = true)
        assertEquals("/ministries", path)
        assertEquals("true", active)
    }

    @Test
    fun getMinistryDetail_mapsAllNestedFields() = runTest {
        val json = testJson.encodeToString(ApiResponse(success = true, data = detail))
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistryDetail("m1")

        val d = result.getOrThrow()
        assertEquals("이 사역은...", d.about)
        assertEquals(listOf("큐베세 양육 수료자"), d.requirements)
        assertEquals(1, d.schedules.size)
        assertEquals("07:00", d.schedules[0].startTime)
        assertEquals("09:00", d.schedules[0].endTime)
        assertEquals(2, d.contacts.size)
        assertEquals("간사", d.contacts[1].role)
        assertEquals(true, d.isActive)
    }

    @Test
    fun getMinistryDetail_requestsCorrectPath() = runTest {
        var path = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = detail))
        val client = mockClient(json) { req -> path = req.url.encodedPath }
        MinistryRepositoryImpl(client).getMinistryDetail("m1")
        assertTrue(path.endsWith("/ministries/m1"), "path was $path")
    }

    // ─── Webapp-created records: optional fields omitted/null must not break parsing ──

    @Test
    fun getMinistries_webappRecordWithOmittedOptionalFields_parses() = runTest {
        // A ministry created from the webapp with no subtitle and a contact that
        // only has a name (no role). Previously this threw MissingFieldException
        // and failed the whole list.
        val json = """
            {"success":true,"data":[
              {"publicId":"m2","title":"새 사역","active":true,
               "contacts":[{"name":"홍길동"}]}
            ]}
        """.trimIndent()
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistries()

        val ministries = result.getOrThrow()
        assertEquals(1, ministries.size)
        assertEquals("m2", ministries[0].publicId)
        assertEquals("새 사역", ministries[0].title)
        assertEquals("", ministries[0].subtitle)
        assertEquals(1, ministries[0].contacts.size)
        assertEquals("홍길동", ministries[0].contacts[0].name)
        assertEquals("", ministries[0].contacts[0].role)
    }

    @Test
    fun getMinistryDetail_webappRecordWithOmittedOptionalFields_parses() = runTest {
        // Detail with no subtitle/about and a schedule with no end time.
        val json = """
            {"success":true,"data":
              {"publicId":"m2","title":"새 사역","active":true,
               "schedules":[{"description":"매주 주일","startTime":"10:00"}]}
            }
        """.trimIndent()
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistryDetail("m2")

        val d = result.getOrThrow()
        assertEquals("m2", d.publicId)
        assertEquals("", d.subtitle)
        assertEquals("", d.about)
        assertEquals(1, d.schedules.size)
        assertEquals("매주 주일", d.schedules[0].description)
        assertEquals("10:00", d.schedules[0].startTime)
        assertEquals("", d.schedules[0].endTime)
    }
}
