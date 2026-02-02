package com.hanmaum.dn.mobile.features.login.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.core.network.NetworkClient
import com.hanmaum.dn.mobile.core.util.AppConfig
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import io.ktor.client.call.*
import io.ktor.client.plugins.expectSuccess
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

    override suspend fun register(request: RegisterRequest): Result<Unit> {
        // Passe die URL an deine Umgebung an!
        // Android Emulator: http://10.0.2.2:8080/api/members
        // iOS Simulator / Echtes Gerät: Deine lokale IP (z.B. http://192.168.1.50:8080/api/members)
        return try {
            val registerUrl = "${AppConfig.getBackendUrl()}/members/register"


            val response = client.post(registerUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
                // WICHTIG: Verhindert, dass Ktor bei 4xx automatisch eine Exception wirft.
                // Wir wollen den Body selbst lesen!
                expectSuccess = false
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                // Nur wenn ALLES gut ging, versuchen wir JSON zu parsen
                // Da wir Result<Unit> zurückgeben, ignorieren wir den Body eigentlich,
                // aber zur Sicherheit lesen wir ihn, falls die API Validierungsfehler als 200 OK sendet (selten).

                // Optional: Falls du sicher bist, dass bei 200 OK alles passt, kannst du das .body() weglassen
                // und direkt Result.success(Unit) zurückgeben.
                // Aber falls dein Backend bei Erfolg JSON sendet:
                try {
                    val apiResponse = response.body<ApiResponse<Unit>>()
                    if (apiResponse.success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(apiResponse.message ?: "Unbekannter Fehler"))
                    }
                } catch(e: Exception) {
                    // Fallback, falls Response OK war aber kein ApiResponse Body
                    Result.success(Unit)
                }

            } else {
                // BEI FEHLER (400, 401, 500):
                // KEIN .body<ApiResponse>() aufrufen! Das verursacht den Crash.
                // Wir lesen den rohen Text.
                val errorText = response.bodyAsText()
                Result.failure(Exception("Fehler (${response.status.value}): $errorText"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Netzwerkfehler (Kein Internet, Server down)
            Result.failure(e)
        }
    }
}