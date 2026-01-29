package com.hanmaum.dn.mobile.features.login.data.repository

import com.hanmaum.dn.mobile.core.network.NetworkClient
import com.hanmaum.dn.mobile.core.util.AppConfig
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthRepositoryImpl : AuthRepository {
    private val client = NetworkClient.client

    override suspend fun login(user: String, pass: String): TokenResponse {
        val keycloakBase = AppConfig.getKeycloakUrl()
        val keycloakUrl = "$keycloakBase/realms/hanmaum/protocol/openid-connect/token"

        val response: HttpResponse = client.submitForm(
            url = keycloakUrl,
            formParameters = Parameters.build {
                append("client_id", "hanmaum-mobile")
                append("grant_type", "password")
                append("username", user)
                append("password", pass)
            }
        )

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val errorBody = response.bodyAsText()
            throw Exception("Login fehlgeschlagen (${response.status.value}): $errorBody")
        }
    }

    override suspend fun testBackendConnection(token: String): String {
        val url = "${AppConfig.getBackendUrl()}/announcements"
        val response = client.get(url) {
            headers { append(HttpHeaders.Authorization, "Bearer $token") }
        }
        return response.status.toString()
    }
}