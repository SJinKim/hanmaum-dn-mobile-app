package com.hanmaum.dn.mobile.core.security

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64

/** Cache identity only. This reads stored claims; it does not validate or authorize a token. */
fun TokenStorage.currentSessionScope(): String? = runCatching {
    val payload = getAccessToken()?.split('.')?.getOrNull(1) ?: return null
    val claims = Json.parseToJsonElement(
        Base64.UrlSafe.decode(payload.padEnd((payload.length + 3) / 4 * 4, '=')).decodeToString(),
    ).jsonObject
    val subject = claims["sub"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank) ?: return null
    val issuer = claims["iss"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank) ?: return null
    "${BuildKonfig.BACKEND_URL}|$issuer|$subject"
}.getOrNull()
