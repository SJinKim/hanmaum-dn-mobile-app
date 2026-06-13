package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.session.SessionManager
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.forms.submitForm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
private data class RefreshTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

/**
 * Forces Ktor's BearerAuthProvider to drop its cached BearerTokens so the next
 * outbound request re-invokes `loadTokens` and picks up whatever is currently
 * in TokenStorage. Call this right after writing fresh tokens (e.g. after a
 * successful login) to avoid the provider replaying stale tokens that were
 * cached at app startup.
 */
fun HttpClient.invalidateBearerCache() {
    authProviders
        .filterIsInstance<BearerAuthProvider>()
        .forEach { provider -> provider.clearToken() }
}

fun createHttpClient(
    tokenStorage: TokenStorage,
    sessionManager: SessionManager,
): HttpClient {
    // Separate plain client for token refresh — no auth interceptor (avoids circular calls)
    val refreshClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.DEFAULT
        }

        defaultRequest {
            if (url.host.isBlank()) {
                val originalPath = url.encodedPath.removePrefix("/")
                url.takeFrom(BuildKonfig.BACKEND_URL)
                url.encodedPath = "/api/v1/" + originalPath
            }
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenStorage.getAccessToken()
                    val refresh = tokenStorage.getRefreshToken()
                    if (access != null && refresh != null) BearerTokens(access, refresh)
                    else null
                }

                refreshTokens {
                    val refreshToken = tokenStorage.getRefreshToken()
                        ?: return@refreshTokens null
                    try {
                        val response = refreshClient.submitForm(
                            url = "${BuildKonfig.KEYCLOAK_URL}/realms/${BuildKonfig.KEYCLOAK_REALM}/protocol/openid-connect/token",
                            formParameters = parameters {
                                append("client_id", "hanmaum-mobile")
                                append("grant_type", "refresh_token")
                                append("refresh_token", refreshToken)
                                // Keep an offline ("keep me signed in") session
                                // offline across rotation; omit for a normal
                                // short-lived SSO session.
                                if (tokenStorage.isKeepSignedIn()) {
                                    append("scope", "offline_access")
                                }
                            }
                        )
                        when {
                            response.status == HttpStatusCode.OK -> {
                                val tokens = response.body<RefreshTokenResponse>()
                                tokenStorage.saveAccessToken(tokens.accessToken)
                                tokens.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                                BearerTokens(tokens.accessToken, tokens.refreshToken ?: refreshToken)
                            }
                            // Keycloak definitively rejected the refresh/offline
                            // token (expired or revoked). The session is over —
                            // tear it down through the canonical pipeline so the
                            // user is cleanly routed back to login.
                            response.status == HttpStatusCode.BadRequest ||
                                response.status == HttpStatusCode.Unauthorized -> {
                                sessionManager.onRefreshRejected()
                                null
                            }
                            // Transient (5xx etc.). Don't destroy the session —
                            // let the 401 surface so the caller can retry later.
                            else -> null
                        }
                    } catch (e: Exception) {
                        // Network error — transient. Never sign the user out here.
                        null
                    }
                }

                sendWithoutRequest { request ->
                    val host = request.url.host
                    val backendHost = Url(BuildKonfig.BACKEND_URL).host
                    val path = request.url.encodedPath
                    val isBackend = host.isBlank() || host == backendHost
                    val isAuthEndpoint = path.contains("register") || path.contains("openid-connect")
                    isBackend && !isAuthEndpoint
                }
            }
        }
    }
}