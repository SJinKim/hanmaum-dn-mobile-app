package com.hanmaum.dn.mobile.core.security

import com.hanmaum.dn.mobile.BuildKonfig
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import kotlin.io.encoding.Base64

/** Public client configuration shared by login and both refresh paths. No client secret. */
object MobileAuthConfig {
    val clientId: String get() = BuildKonfig.KEYCLOAK_CLIENT_ID
    val redirectUri: String get() = BuildKonfig.KEYCLOAK_REDIRECT_URI
    val issuer: String get() = "${BuildKonfig.KEYCLOAK_URL.trimEnd('/')}/realms/${BuildKonfig.KEYCLOAK_REALM}"
    val tokenEndpoint: String get() = "$issuer/protocol/openid-connect/token"
}

internal fun base64Url(bytes: ByteArray): String = Base64.UrlSafe.encode(bytes).trimEnd('=')

/** In-memory, one-use transaction. Never persist or stringify the verifier/state. */
class PkceAuthorization(
    private val issuer: String = MobileAuthConfig.issuer,
    private val clientId: String = MobileAuthConfig.clientId,
    val redirectUri: String = MobileAuthConfig.redirectUri,
    private val random: (Int) -> ByteArray = ::secureRandomBytes,
) {
    private var verifier: String? = null
    private var state: String? = null

    fun begin(uiLocale: String? = null): String {
        check(state == null) { "Authorization already running" }
        val nextVerifier = base64Url(random(32))
        val nextState = base64Url(random(32))
        verifier = nextVerifier
        state = nextState
        return URLBuilder("$issuer/protocol/openid-connect/auth").apply {
            parameters.append("client_id", clientId)
            parameters.append("redirect_uri", redirectUri)
            parameters.append("response_type", "code")
            parameters.append("scope", "openid offline_access")
            parameters.append("code_challenge_method", "S256")
            parameters.append("code_challenge", base64Url(sha256(nextVerifier.encodeToByteArray())))
            parameters.append("state", nextState)
            // A local logout must not silently sign the next person in via browser SSO.
            // Offline/Face ID sessions remain usable; no server-wide logout is issued.
            parameters.append("prompt", "login")
            uiLocale?.let { parameters.append("ui_locales", it) }
        }.buildString()
    }

    fun cancel() {
        verifier = null
        state = null
    }

    /** All callbacks consume the transaction, including malformed/error callbacks. */
    fun complete(callback: String): AuthorizationCode {
        val expectedState = state
        val codeVerifier = verifier
        cancel()
        require(expectedState != null && codeVerifier != null) { "No pending authorization" }
        require(!callback.contains('#')) { "Unexpected callback fragment" }
        require(callback.substringBefore('?') == redirectUri) { "Unexpected redirect URI" }
        val parameters = parseQueryString(callback.substringAfter('?', ""))
        require(parameters.getAll("state") == listOf(expectedState)) { "Invalid authorization state" }
        parameters.getAll("iss")?.let { require(it == listOf(issuer)) { "Unexpected issuer" } }
        require(parameters.getAll("error") == null) { "Authorization refused" }
        val codes = parameters.getAll("code")
        require(codes?.size == 1 && !codes.single().isBlank()) { "Missing authorization code" }
        return AuthorizationCode(codes.single(), codeVerifier)
    }

    val callbackScheme: String get() = redirectUri.substringBefore(':')
}

/** Not a data class: default toString must not expose code/verifier. */
class AuthorizationCode(val code: String, val verifier: String)
