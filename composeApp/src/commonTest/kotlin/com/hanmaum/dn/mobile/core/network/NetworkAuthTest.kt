package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class SessionTokens : TokenStorage {
    var access: String? = "access"
    var refresh: String? = "refresh"
    override fun saveAccessToken(token: String) { access = token }
    override fun getAccessToken() = access
    override fun saveRefreshToken(token: String?) { refresh = token }
    override fun getRefreshToken() = refresh
    override fun clear() { access = null; refresh = null }
}

class NetworkAuthTest {
    @Test fun foreignBearerChallengeNeverGetsSessionCredentials() = runTest {
        var requests = 0
        val client = createHttpClient(SessionTokens(), MockEngine { request ->
            requests++
            assertNull(request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
        })
        assertEquals(HttpStatusCode.Unauthorized, client.get("https://foreign.example.org/private").status)
        client.close()
        assertEquals(1, requests)
    }

    @Test fun backend401RefreshesOnceAndRetriesWithRotatedAccess() = runTest {
        val tokens = SessionTokens()
        val bearers = mutableListOf<String?>()
        var refreshes = 0
        val client = createHttpClient(tokens, MockEngine { request ->
            if (request.url.encodedPath.contains("openid-connect")) {
                refreshes++
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("""{"access_token":"new-access","refresh_token":"new-refresh"}""",
                    HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                bearers += request.headers[HttpHeaders.Authorization]
                if (bearers.size == 1) respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
                else respond("", HttpStatusCode.OK)
            }
        })
        assertEquals(HttpStatusCode.OK, client.get("${BuildKonfig.BACKEND_URL}/api/v1/me").status)
        assertEquals(listOf<String?>("Bearer access", "Bearer new-access"), bearers)
        assertEquals(1, refreshes)
        assertEquals("new-refresh", tokens.refresh)
        client.close()
    }

    @Test fun backend403DoesNotRefresh() = runTest {
        var requests = 0
        val client = createHttpClient(SessionTokens(), MockEngine { request ->
            requests++
            assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.Forbidden)
        })
        assertEquals(HttpStatusCode.Forbidden, client.get("${BuildKonfig.BACKEND_URL}/api/v1/me").status)
        assertEquals(1, requests)
        client.close()
    }

    @Test fun localLogoutDropsCachedBearerBeforeNextRequest() = runTest {
        val tokens = SessionTokens()
        val bearers = mutableListOf<String?>()
        val client = createHttpClient(tokens, MockEngine { request ->
            bearers += request.headers[HttpHeaders.Authorization]
            respond("", HttpStatusCode.OK)
        })
        client.get("${BuildKonfig.BACKEND_URL}/api/v1/me")
        tokens.clear()
        client.invalidateBearerCache()
        client.get("${BuildKonfig.BACKEND_URL}/api/v1/me")
        assertEquals(listOf("Bearer access", null), bearers)
        client.close()
    }

    @Test fun rejectedRefreshLeavesBackend401WithoutInfiniteRetry() = runTest {
        var refreshes = 0
        var apiRequests = 0
        val client = createHttpClient(SessionTokens(), MockEngine { request ->
            if (request.url.encodedPath.contains("openid-connect")) {
                refreshes++
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest,
                    headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                apiRequests++
                respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
            }
        })
        assertEquals(HttpStatusCode.Unauthorized, client.get("${BuildKonfig.BACKEND_URL}/api/v1/me").status)
        assertEquals(1, refreshes)
        assertEquals(1, apiRequests)
        client.close()
    }

    @Test fun redirectToForeignHostStripsTheBackendBearer() = runTest {
        val backendHost = Url(BuildKonfig.BACKEND_URL).host
        var foreignRequests = 0
        val client = createHttpClient(SessionTokens(), MockEngine { request ->
            if (request.url.host == backendHost) {
                assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://foreign.example.org/target"))
            } else {
                foreignRequests++
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("", HttpStatusCode.OK)
            }
        })
        assertEquals(HttpStatusCode.OK, client.get("${BuildKonfig.BACKEND_URL}/api/v1/me").status)
        assertEquals(1, foreignRequests)
        client.close()
    }
}
