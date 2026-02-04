package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

object NetworkClient {
    // Wir brauchen Zugriff auf den Storage
    val tokenStorage: TokenStorage = TokenStorageImpl()

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = tokenStorage.getAccessToken()
                    val refreshToken = tokenStorage.getRefreshToken()
                    if (accessToken != null) {
                        BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
                    } else {
                        null
                    }
                }
                refreshTokens {
                    null
                }
            }
        }
    }
}