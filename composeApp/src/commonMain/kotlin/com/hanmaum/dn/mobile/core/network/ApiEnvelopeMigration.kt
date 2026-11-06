package com.hanmaum.dn.mobile.core.network

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@PublishedApi
internal val migrationResponseJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Only for the four legacy GETs being migrated in server issue 285. */
internal suspend inline fun <reified T : Any> HttpResponse.legacyOrWrappedBody(): T {
    check(status.isSuccess()) { "Unexpected status: $status" }
    val payload = body<JsonElement>()
    return if (payload is JsonObject && "success" in payload) {
        val envelope = migrationResponseJson.decodeFromJsonElement<ApiResponse<T>>(payload)
        check(envelope.success) { "Backend response was not successful" }
        checkNotNull(envelope.data) { "Backend response contained no data" }
    } else {
        migrationResponseJson.decodeFromJsonElement<T>(payload)
    }
}
