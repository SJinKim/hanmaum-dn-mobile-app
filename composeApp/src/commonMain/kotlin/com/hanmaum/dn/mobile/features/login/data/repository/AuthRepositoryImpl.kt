package com.hanmaum.dn.mobile.features.login.data.repository

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.login.domain.model.KeycloakError
import com.hanmaum.dn.mobile.features.login.domain.model.LoginException
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

    /**
     * Signs in and asks for an *offline* session.
     *
     * Without `offline_access` the refresh token belongs to the SSO session,
     * which the realm ends after 30 minutes idle and 10 hours at the latest. The
     * Face ID vault seals that very token, so Face ID could not survive a night
     * however correct the app was, and neither could "keep me signed in" (#231).
     * An offline token is bound to the offline session instead — 30 days idle in
     * this realm.
     *
     * It is asked for on every sign-in, not only when "keep me signed in" is on:
     * Face ID is armed later, from 설정, and seals whatever refresh token is in
     * hand at that moment. Handing it a short-lived one would arm something that
     * quietly expires. Signing out still ends the session on this device — the
     * app drops the tokens; what it deliberately keeps is the sealed copy, which
     * only this member's face opens (#220).
     */
    override suspend fun login(user: String, pass: String): TokenResponse =
        tokenRequest {
            append("client_id", "hanmaum-mobile")
            append("grant_type", "password")
            append("username", user)
            append("password", pass)
            append("scope", "openid offline_access")
        }

    /** Both grants hit the same endpoint and fail the same way. */
    private suspend fun tokenRequest(form: ParametersBuilder.() -> Unit): TokenResponse {
        val keycloakUrl = "${BuildKonfig.KEYCLOAK_URL}/realms/${BuildKonfig.KEYCLOAK_REALM}/protocol/openid-connect/token"

        val response: HttpResponse = client.submitForm(
            url = keycloakUrl,
            formParameters = Parameters.build(form),
        )

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            // Keycloak answers a refusal with {error, error_description}. Keeping
            // the two apart lets the caller tell "wrong password" from "you have
            // not confirmed your email yet" — the latter is actionable, and used
            // to arrive as an opaque string (#168).
            val errorBody = response.bodyAsText()
            val parsed = runCatching { lenientJson.decodeFromString<KeycloakError>(errorBody) }.getOrNull()
            throw LoginException(
                status = response.status.value,
                error = parsed?.error,
                description = parsed?.errorDescription,
            )
        }
    }

    override suspend fun refresh(refreshToken: String): TokenResponse =
        tokenRequest {
            append("client_id", "hanmaum-mobile")
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
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
