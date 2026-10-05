package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.security.MobileAuthConfig
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.statement.request
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
import kotlinx.coroutines.CancellationException


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

/**
 * Whether the Keycloak bearer may ride along on this request.
 *
 * Scoped by **host**, not by path: the app also talks to googleapis.com and S3,
 * and handing them a church access token both leaks it and breaks those APIs
 * (they reject a foreign Authorization header). Extracted from the plugin so
 * the rule can be tested without standing up a client.
 */
internal fun shouldSendBearer(
    requestHost: String,
    requestPath: String,
    backendHost: String,
): Boolean {
    // A blank host means the relative-URL branch of defaultRequest is about to
    // rewrite it to the backend.
    val isBackend = requestHost.isBlank() || requestHost == backendHost
    val isAuthEndpoint = requestPath.contains("register") || requestPath.contains("openid-connect")
    return isBackend && !isAuthEndpoint
}

fun createHttpClient(tokenStorage: TokenStorage, engine: HttpClientEngine? = null): HttpClient {
    val clientConfig: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.DEFAULT
            sanitizeHeader { it == HttpHeaders.Authorization }
            filter { !it.url.encodedPath.contains("openid-connect") }
        }

        defaultRequest {
            if (url.host.isBlank()) {
                val originalPath = url.encodedPath.removePrefix("/")
                url.takeFrom(BuildKonfig.BACKEND_URL)
                url.encodedPath = "/api/v1/" + originalPath
            }
        }

        install(Auth) {
            reAuthorizeOnResponse { response ->
                response.status == HttpStatusCode.Unauthorized && shouldSendBearer(
                    response.request.url.host, response.request.url.encodedPath, Url(BuildKonfig.BACKEND_URL).host,
                )
            }
            bearer {
                loadTokens {
                    val access = tokenStorage.getAccessToken()
                    val refresh = tokenStorage.getRefreshToken()
                    if (access != null && refresh != null) BearerTokens(access, refresh)
                    else null
                }

                refreshTokens {
                    if (!shouldSendBearer(response.request.url.host, response.request.url.encodedPath,
                            Url(BuildKonfig.BACKEND_URL).host)) return@refreshTokens null
                    val refreshToken = tokenStorage.getRefreshToken()
                        ?: return@refreshTokens null
                    try {
                        val response = client.submitForm(
                            url = MobileAuthConfig.tokenEndpoint,
                            formParameters = parameters {
                                append("client_id", MobileAuthConfig.clientId)
                                append("grant_type", "refresh_token")
                                append("refresh_token", refreshToken)
                            }
                        ) { markAsRefreshTokenRequest() }
                        if (response.status == HttpStatusCode.OK) {
                            val tokens = response.body<RefreshTokenResponse>()
                            tokenStorage.saveAccessToken(tokens.accessToken)
                            tokens.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                            BearerTokens(tokens.accessToken, tokens.refreshToken ?: refreshToken)
                        } else {
                            // Don't clear storage here — a transient refresh failure
                            // must not destroy tokens that were just saved by a
                            // concurrent login. Returning null lets the 401 surface
                            // to the caller, which decides whether to re-auth.
                            null
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                }

                sendWithoutRequest { request ->
                    shouldSendBearer(
                        requestHost = request.url.host,
                        requestPath = request.url.encodedPath,
                        backendHost = Url(BuildKonfig.BACKEND_URL).host,
                    )
                }
            }
        }
    }
    return (if (engine == null) HttpClient(clientConfig) else HttpClient(engine, clientConfig)).apply {
        // sendWithoutRequest controls preemptive auth only. A foreign server can
        // challenge with WWW-Authenticate; strip again on every send/retry.
        plugin(HttpSend).intercept { request ->
            if (!shouldSendBearer(request.url.host, request.url.encodedPath, Url(BuildKonfig.BACKEND_URL).host)) {
                request.headers.remove(HttpHeaders.Authorization)
            }
            execute(request)
        }
    }
}
