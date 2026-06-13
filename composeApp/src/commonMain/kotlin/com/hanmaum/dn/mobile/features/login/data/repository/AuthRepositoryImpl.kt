package com.hanmaum.dn.mobile.features.login.data.repository

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterException
import com.hanmaum.dn.mobile.features.login.domain.model.RegisterRequest
import com.hanmaum.dn.mobile.features.login.domain.model.TokenResponse
import com.hanmaum.dn.mobile.features.login.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class AuthRepositoryImpl(
    private val client: HttpClient
) : AuthRepository {

    private val lenientJson = Json { ignoreUnknownKeys = true }

    override suspend fun login(user: String, pass: String, keepSignedIn: Boolean): TokenResponse {
        val keycloakUrl = "${BuildKonfig.KEYCLOAK_URL}/realms/${BuildKonfig.KEYCLOAK_REALM}/protocol/openid-connect/token"

        val response: HttpResponse = client.submitForm(
            url = keycloakUrl,
            formParameters = Parameters.build {
                append("client_id", "hanmaum-mobile")
                append("grant_type", "password")
                append("username", user)
                append("password", pass)
                // "Keep me signed in" → request an offline token so the session
                // survives app restart and long idle (governed by Keycloak's
                // Offline Session Idle, default 30 days) instead of the short
                // SSO Session Idle. Omitted otherwise for a normal session.
                if (keepSignedIn) {
                    append("scope", "offline_access")
                }
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
        return try {
            val response = client.post("members/register") {
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
                        Result.failure(RegisterException(apiResponse.message))
                    }
                } catch(e: Exception) {
                    // Fallback, falls Response OK war aber kein ApiResponse Body
                    Result.success(Unit)
                }

            } else {
                // BEI FEHLER (400, 401, 500):
                // Body als Text lesen (kein .body<ApiResponse>() -> Crash bei falschem Content-Type),
                // dann sicher als ApiResponse parsen, um eine saubere Nachricht zu extrahieren.
                val errorText = response.bodyAsText()
                val message = runCatching {
                    lenientJson.decodeFromString<ApiResponse<Unit>>(errorText).message
                }.getOrNull()
                // null/blank -> ViewModel zeigt eine generische, lokalisierte Meldung.
                Result.failure(RegisterException(message))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Netzwerkfehler (Kein Internet, Server down) -> generische Meldung.
            Result.failure(RegisterException(null))
        }
    }
}
