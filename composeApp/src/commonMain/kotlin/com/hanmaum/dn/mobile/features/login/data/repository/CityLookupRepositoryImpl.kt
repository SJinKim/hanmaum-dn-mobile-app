package com.hanmaum.dn.mobile.features.login.data.repository

import com.hanmaum.dn.mobile.features.login.domain.repository.CityLookupRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import kotlinx.serialization.Serializable

/**
 * Looks up the city for a German PLZ via the free OpenPLZ API
 * (https://openplzapi.org) — no API key, no bundled dataset.
 *
 * The absolute URL bypasses the shared client's base-URL injection, and the
 * non-backend host means the bearer-auth plugin never attaches a token. Any
 * failure (offline, 5xx, malformed body) resolves to null so the form falls
 * back to manual city entry.
 */
class CityLookupRepositoryImpl(
    private val client: HttpClient,
) : CityLookupRepository {

    override suspend fun cityForPostalCode(postalCode: String): String? {
        val plz = postalCode.trim()
        if (plz.isEmpty()) return null
        return runCatching {
            val localities: List<OpenPlzLocality> =
                client.get("https://openplzapi.org/de/Localities") {
                    parameter("postalCode", plz)
                    accept(ContentType.Application.Json)
                }.body()
            localities.firstOrNull()?.name
        }.getOrNull()
    }

    @Serializable
    private data class OpenPlzLocality(val name: String)
}
