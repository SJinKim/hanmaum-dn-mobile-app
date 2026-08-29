package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeTokenStorage(
    private var access: String? = "access-token-abc",
    private var refresh: String? = "refresh-token-xyz",
) : TokenStorage {
    override fun saveAccessToken(token: String) { access = token }
    override fun getAccessToken() = access
    override fun saveRefreshToken(token: String?) { refresh = token }
    override fun getRefreshToken() = refresh
    override fun clear() { access = null; refresh = null }
}

/**
 * The Google Calendar and pCloud repositories share the authenticated client,
 * so the Keycloak token must never reach them — not proactively, and not on the
 * retry Ktor performs after a 401 challenge.
 */
class NetworkClientAuthScopeTest {

    @Test
    fun externalHostChallengeDoesNotReplayTheTokenAtIt() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.Unauthorized,
                // A bearer challenge is exactly what would make Ktor
                // re-authorise and retry with our token attached.
                headers = headersOf(HttpHeaders.WWWAuthenticate, "Bearer realm=\"external\""),
            )
        }
        val client = createHttpClient(FakeTokenStorage(), engine)

        val response = client.get("https://www.googleapis.com/calendar/v3/calendars/x/events")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(
            engine.requestHistory.none { it.headers.contains(HttpHeaders.Authorization) },
            "the bearer token was sent to an external host: " +
                engine.requestHistory.map { it.url.host to it.headers[HttpHeaders.Authorization] },
        )
        assertEquals(1, engine.requestHistory.size, "the 401 must surface, not trigger a retry")
    }

    @Test
    fun backendRequestsStillCarryTheToken() = runTest {
        val engine = MockEngine { _ -> respond(content = "[]", status = HttpStatusCode.OK) }
        val client = createHttpClient(FakeTokenStorage(), engine)

        client.get("announcements")

        val request = engine.requestHistory.single()
        assertEquals(Url(BuildKonfig.BACKEND_URL).host, request.url.host)
        assertEquals("Bearer access-token-abc", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun externalRequestsNeverCarryTheTokenProactively() = runTest {
        val engine = MockEngine { _ -> respond(content = "{}", status = HttpStatusCode.OK) }
        val client = createHttpClient(FakeTokenStorage(), engine)

        client.get("https://api.pcloud.com/listfolder?code=abc")

        assertEquals(null, engine.requestHistory.single().headers[HttpHeaders.Authorization])
    }
}
