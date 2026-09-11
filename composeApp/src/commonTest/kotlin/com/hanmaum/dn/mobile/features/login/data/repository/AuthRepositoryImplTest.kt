package com.hanmaum.dn.mobile.features.login.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.OutgoingContent
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOKEN_BODY = """
    {"access_token":"a","refresh_token":"r","expires_in":300,"token_type":"Bearer"}
"""

/** Captures what actually went to Keycloak, rather than what was intended. */
private class CapturingEngine {
    var lastForm: Parameters = Parameters.Empty
        private set

    val client = HttpClient(
        MockEngine { request: HttpRequestData ->
            // submitForm sends the parameters as a byte-array body, not text.
            lastForm = (request.body as? OutgoingContent.ByteArrayContent)
                ?.bytes()
                ?.decodeToString()
                ?.let { parseQueryString(it) }
                ?: Parameters.Empty
            respond(
                content = TOKEN_BODY,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
}

class AuthRepositoryImplTest {

    @Test
    fun `signing in asks for an offline session`() = runTest {
        // Without it the refresh token dies with the SSO session — 30 minutes
        // idle in this realm — and the Face ID vault seals exactly that token,
        // so a night is enough to make Face ID useless (#231).
        val engine = CapturingEngine()

        AuthRepositoryImpl(engine.client).login("member@example.org", "secret")

        val scope = engine.lastForm["scope"].orEmpty()
        assertTrue("offline_access" in scope, "scope was '$scope'")
    }

    @Test
    fun `signing in still sends the password grant unchanged`() = runTest {
        val engine = CapturingEngine()

        AuthRepositoryImpl(engine.client).login("member@example.org", "secret")

        assertEquals("password", engine.lastForm["grant_type"])
        assertEquals("hanmaum-mobile", engine.lastForm["client_id"])
        assertEquals("member@example.org", engine.lastForm["username"])
        assertEquals("secret", engine.lastForm["password"])
    }

    @Test
    fun `refreshing asks for no scope of its own`() = runTest {
        // A refresh inherits the session it came from; naming a scope here would
        // narrow it instead, and an offline token would quietly become a normal
        // one on the first silent refresh.
        val engine = CapturingEngine()

        AuthRepositoryImpl(engine.client).refresh("sealed-refresh")

        assertEquals("refresh_token", engine.lastForm["grant_type"])
        assertEquals(null, engine.lastForm["scope"])
    }
}
