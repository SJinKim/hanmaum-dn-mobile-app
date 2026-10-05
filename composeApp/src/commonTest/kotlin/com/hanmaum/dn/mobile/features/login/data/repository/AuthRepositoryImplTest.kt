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
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterException
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.LoginException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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

private val REGISTER_REQUEST = RegisterRequest(
    firstName = "승진",
    lastName = "김",
    email = "hello@hanmaum.de",
    city = "Düsseldorf",
    password = "Passwort1!",
)

private fun registerClient(status: HttpStatusCode, body: String) = HttpClient(
    MockEngine {
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    },
) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

class AuthRepositoryImplTest {

    @Test
    fun `code exchange sends no password or client secret`() = runTest {
        val engine = CapturingEngine()

        AuthRepositoryImpl(engine.client).exchangeAuthorizationCode("one-use-code", "verifier")

        assertEquals(null, engine.lastForm["password"])
        assertEquals(null, engine.lastForm["username"])
        assertEquals(null, engine.lastForm["client_secret"])
    }

    @Test
    fun `signing in exchanges the code and PKCE verifier`() = runTest {
        val engine = CapturingEngine()

        AuthRepositoryImpl(engine.client).exchangeAuthorizationCode("one-use-code", "verifier")

        assertEquals("authorization_code", engine.lastForm["grant_type"])
        assertEquals("hanmaum-mobile", engine.lastForm["client_id"])
        assertEquals("one-use-code", engine.lastForm["code"])
        assertEquals("verifier", engine.lastForm["code_verifier"])
        assertEquals("com.hanmaum.dn.mobile:/oauth2redirect", engine.lastForm["redirect_uri"])
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

    @Test
    fun `rejected code keeps structured invalid grant without saving a session`() = runTest {
        val client = registerClient(HttpStatusCode.BadRequest,
            """{"error":"invalid_grant","error_description":"Code expired"}""")
        val error = runCatching { AuthRepositoryImpl(client).exchangeAuthorizationCode("old-code", "v") }.exceptionOrNull()
        assertIs<LoginException>(error)
        assertEquals("invalid_grant", error.error)
        assertEquals(400, error.status)
        client.close()
    }

    @Test
    fun `a server failure on register is marked as one`() = runTest {
        val client = registerClient(
            HttpStatusCode.InternalServerError,
            """{"success":false,"message":"Internal error","data":null}""",
        )

        val error = AuthRepositoryImpl(client).register(REGISTER_REQUEST).exceptionOrNull()

        assertIs<RegisterException>(error)
        assertTrue(error.isServerError)
    }

    @Test
    fun `a refused register keeps its message and is not a server failure`() = runTest {
        val client = registerClient(
            HttpStatusCode.Conflict,
            """{"success":false,"message":"Email already registered","data":null}""",
        )

        val error = AuthRepositoryImpl(client).register(REGISTER_REQUEST).exceptionOrNull()

        assertIs<RegisterException>(error)
        assertFalse(error.isServerError)
        assertEquals("Email already registered", error.userMessage)
    }
}
