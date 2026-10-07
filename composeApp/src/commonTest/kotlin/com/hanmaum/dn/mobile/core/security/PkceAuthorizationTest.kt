package com.hanmaum.dn.mobile.core.security

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PkceAuthorizationTest {
    private val issuer = "https://auth.example.org/realms/st"
    private val redirect = "com.hanmaum.dn.mobile:/oauth2redirect"
    private fun transaction() = PkceAuthorization(issuer, "mobile", redirect)

    @Test fun authorizationCarriesTheChosenResetPageLanguage() {
        listOf("ko", "en", "de").forEach { locale ->
            val params = Url(transaction().begin(locale)).parameters
            assertEquals(locale, params["ui_locales"])
            assertEquals("S256", params["code_challenge_method"])
            assertEquals(redirect, params["redirect_uri"])
        }
        assertEquals(null, Url(transaction().begin()).parameters["ui_locales"])
    }

    @Test fun s256MatchesRfc7636Vector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            base64Url(sha256("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk".encodeToByteArray())))
    }

    @Test fun authorizationRequestsOfflineCodeFlowWithoutSecrets() {
        val params = Url(transaction().begin()).parameters
        assertEquals("S256", params["code_challenge_method"])
        assertEquals("code", params["response_type"])
        assertEquals("openid offline_access", params["scope"])
        assertEquals("login", params["prompt"])
        assertEquals(43, params["code_challenge"]?.length)
        assertEquals(43, params["state"]?.length)
        assertEquals(null, params["code_verifier"])
        assertEquals(null, params["client_secret"])
        assertNotEquals(params["state"], params["code_challenge"])
    }

    @Test fun validCallbackIsOneUseAndKeepsVerifierOffWire() {
        val transaction = transaction()
        val params = Url(transaction.begin()).parameters
        val callback = "$redirect?code=c&state=${params["state"]}"
        val grant = transaction.complete(callback)
        assertEquals("c", grant.code)
        assertEquals(43, grant.verifier.length)
        assertEquals(params["code_challenge"], base64Url(sha256(grant.verifier.encodeToByteArray())))
        assertFailsWith<IllegalArgumentException> { transaction.complete(callback) }
    }

    @Test fun malformedCallbacksConsumeTransactionWithoutAcceptingCodes() {
        listOf<(String) -> String>(
            { "$redirect?code=c&state=other" },
            { "$redirect?code=c" },
            { "$redirect?code=c&state=$it&state=$it" },
            { "$redirect?code=c&code=d&state=$it" },
            { "com.other:/oauth2redirect?code=c&state=$it" },
            { "$redirect/extra?code=c&state=$it" },
            { "$redirect?code=c&state=$it#fragment" },
            { "$redirect?error=access_denied&state=$it" },
            { "$redirect?code=c&state=$it&iss=https%3A%2F%2Fother" },
        ).forEach { callback ->
            val transaction = transaction()
            val state = Url(transaction.begin()).parameters["state"]!!
            assertFailsWith<IllegalArgumentException> { transaction.complete(callback(state)) }
            assertFailsWith<IllegalArgumentException> { transaction.complete("$redirect?code=c&state=$state") }
        }
    }

    @Test fun cancelledTransactionRejectsLateCallbackAndRetryUsesFreshState() {
        val transaction = transaction()
        val before = Url(transaction.begin()).parameters["state"]!!
        transaction.cancel()
        assertFailsWith<IllegalArgumentException> { transaction.complete("$redirect?code=c&state=$before") }
        val after = Url(transaction.begin()).parameters["state"]!!
        assertNotEquals(before, after)
        assertTrue(transaction.complete("$redirect?code=c&state=$after").verifier.isNotBlank())
    }
}
