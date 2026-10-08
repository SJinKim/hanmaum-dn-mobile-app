package com.hanmaum.dn.mobile.core.security

import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse

private class ScopeTokens(private var token: String?) : TokenStorage {
    override fun getAccessToken(): String? = token
    override fun saveAccessToken(token: String) { this.token = token }
    override fun getRefreshToken(): String? = null
    override fun saveRefreshToken(token: String?) = Unit
    override fun clear() { token = null }
}

class SessionScopeTest {
    private fun tokens(claims: String, signature: String = "signature") = ScopeTokens(
        "header.${Base64.UrlSafe.encode(claims.encodeToByteArray()).trimEnd('=')}.$signature",
    )

    @Test fun `cache identity survives token rotation but isolates subject and issuer`() {
        val first = tokens("""{"iss":"realm-a","sub":"member-1"}""").currentSessionScope()
        assertEquals(first, tokens("""{"iss":"realm-a","sub":"member-1","exp":999}""", "rotated").currentSessionScope())
        assertNotEquals(first, tokens("""{"iss":"realm-a","sub":"member-2"}""").currentSessionScope())
        assertNotEquals(first, tokens("""{"iss":"realm-b","sub":"member-1"}""").currentSessionScope())
        assertFalse(first.orEmpty().contains("signature"))
    }

    @Test fun `malformed missing and incomplete sessions have no cache identity`() {
        assertNull(ScopeTokens(null).currentSessionScope())
        assertNull(ScopeTokens("malformed").currentSessionScope())
        assertNull(ScopeTokens("header.not-json.signature").currentSessionScope())
        assertNull(tokens("""{"sub":"member-1"}""").currentSessionScope())
        assertNull(tokens("""{"iss":"realm-a","sub":""}""").currentSessionScope())
    }
}
