package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.encodedPath
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.client.plugins.logging.*


fun createHttpClient(tokenStorage: TokenStorage): HttpClient {
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
                // 1. Sichere den relativen Pfad, den das Repo gesendet hat (z.B. "members/register")
                val originalPath = url.encodedPath.removePrefix("/")

                // 2. Setze die Basis-URL (http://10.0.2.2:8080)
                url.takeFrom(BuildKonfig.BACKEND_URL)

                // 3. Baue den Pfad manuell als String zusammen. Das ist in KMP am sichersten!
                // Ergebnis: /api/v1/members/register
                url.encodedPath = "/api/v1/" + originalPath

                println("JIN: Finaler Request URL: ${url.buildString()}")
            }
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenStorage.getAccessToken()
                    val refresh = tokenStorage.getRefreshToken()
                    if(access != null && refresh != null) {
                        BearerTokens(access, refresh)
                    } else null
                    // TODO: refreshToken Logik
                }
                sendWithoutRequest { request ->
                    val path = request.url.encodedPath

                    // Wir prüfen einfach, ob das Wort "register" oder "openid-connect" im Pfad vorkommt.
                    // Das ist viel robuster als ein exakter Vergleich!
                    val shouldSkipAuth = path.contains("register") || path.contains("openid-connect")

                    println("JIN: Auth-Check für $path -> Skip Auth: $shouldSkipAuth")
                    shouldSkipAuth

                }
            }
        }
    }
}