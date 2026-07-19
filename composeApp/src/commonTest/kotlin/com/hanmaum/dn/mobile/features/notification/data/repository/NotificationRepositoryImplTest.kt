package com.hanmaum.dn.mobile.features.notification.data.repository

import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType
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

class NotificationRepositoryImplTest {

    private val notificationJson = """
        {"success":true,"message":null,"data":{"items":[
          {"publicId":"n1","type":"ANNOUNCEMENT","title":"새로운 소식이 있습니다!","body":"여름 수련회",
           "referenceType":"ANNOUNCEMENT","referencePublicId":"a1",
           "createdAt":"2026-07-15T10:00:00Z","seenAt":null,"readAt":null},
          {"publicId":"n2","type":"PRAYER_ALERT","title":"t","body":"b",
           "referenceType":null,"referencePublicId":null,
           "createdAt":"2026-07-14T10:00:00Z","seenAt":"2026-07-14T11:00:00Z","readAt":"2026-07-14T11:00:00Z"}
        ],"page":0,"hasNext":true}}
    """.trimIndent()

    @Test
    fun `getNotifications hits the right path and maps fields`() = runTest {
        var path = ""
        val repo = NotificationRepositoryImpl(mockClient(notificationJson) { path = it.url.encodedPath + "?" + it.url.encodedQuery })
        val page = repo.getNotifications(page = 0).getOrThrow()
        assertEquals("/me/notifications?page=0&size=20", path)
        assertEquals(2, page.items.size)
        assertTrue(page.hasNext)
        val first = page.items[0]
        assertEquals(NotificationType.ANNOUNCEMENT, first.type)
        assertEquals("a1", first.reference?.publicId)
        assertEquals(false, first.isSeen)
        assertEquals(false, first.isRead)
    }

    @Test
    fun `unknown type maps to UNKNOWN with no reference crash`() = runTest {
        val repo = NotificationRepositoryImpl(mockClient(notificationJson))
        val second = repo.getNotifications(0).getOrThrow().items[1]
        assertEquals(NotificationType.UNKNOWN, second.type)
        assertEquals(null, second.reference)
        assertTrue(second.isRead)
    }

    @Test
    fun `unseen count parses`() = runTest {
        val repo = NotificationRepositoryImpl(mockClient("""{"success":true,"message":null,"data":{"count":3}}"""))
        assertEquals(3, repo.getUnseenCount().getOrThrow())
    }

    @Test
    fun `register device token sends exact body keys`() = runTest {
        var body = ""
        var method = ""
        var path = ""
        val repo = NotificationRepositoryImpl(
            mockClient("""{"success":true,"message":null,"data":null}""") {
                method = it.method.value; path = it.url.encodedPath
                body = (it.body as TextContent).text
            },
        )
        repo.registerDeviceToken("tok123", "ANDROID").getOrThrow()
        assertEquals("PUT", method)
        assertEquals("/me/device-tokens", path)
        assertEquals("""{"token":"tok123","platform":"ANDROID"}""", body)
    }

    @Test
    fun `set push enabled sends exact body`() = runTest {
        var body = ""
        val repo = NotificationRepositoryImpl(
            mockClient("""{"success":true,"message":null,"data":null}""") { body = (it.body as TextContent).text },
        )
        repo.setPushEnabled(false).getOrThrow()
        assertEquals("""{"pushEnabled":false}""", body)
    }

    @Test
    fun `mark read posts to the notification path`() = runTest {
        var path = ""
        var method = ""
        val repo = NotificationRepositoryImpl(
            mockClient("""{"success":true,"message":null,"data":null}""") { path = it.url.encodedPath; method = it.method.value },
        )
        repo.markRead("n1").getOrThrow()
        assertEquals("POST", method)
        assertEquals("/me/notifications/n1/read", path)
    }

    @Test
    fun `server error status maps to failure`() = runTest {
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(testJson) }
        }
        assertTrue(NotificationRepositoryImpl(client).getUnseenCount().isFailure)
    }
}
