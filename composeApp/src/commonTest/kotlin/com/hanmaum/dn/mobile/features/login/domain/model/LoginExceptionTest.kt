package com.hanmaum.dn.mobile.features.login.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Telling a pending required action apart from a wrong password. Both arrive as
 * `invalid_grant`, so the description is the only thing that separates them —
 * and getting it wrong sends a member at a door that will not open (#168).
 */
class LoginExceptionTest {

    @Test
    fun keycloaksWordingForAPendingRequiredActionIsRecognised() {
        assertTrue(
            LoginException(400, "invalid_grant", "Account is not fully set up").isAccountNotFullySetUp,
        )
    }

    @Test
    fun theMatchSurvivesADifferentCaseOrExtraWords() {
        // The phrasing is not part of any contract, so the match is loose on
        // purpose.
        assertTrue(LoginException(400, "invalid_grant", "ACCOUNT IS NOT FULLY SET UP").isAccountNotFullySetUp)
        assertTrue(
            LoginException(400, "invalid_grant", "Account is not fully set up: VERIFY_EMAIL")
                .isAccountNotFullySetUp,
        )
    }

    @Test
    fun aWrongPasswordIsNotConfusedWithIt() {
        assertFalse(
            LoginException(401, "invalid_grant", "Invalid user credentials").isAccountNotFullySetUp,
        )
    }

    @Test
    fun otherRefusalsAreNotConfusedWithItEither() {
        assertFalse(LoginException(401, "invalid_client", "Invalid client credentials").isAccountNotFullySetUp)
        assertFalse(LoginException(400, "unsupported_grant_type", null).isAccountNotFullySetUp)
        assertFalse(LoginException(503, null, null).isAccountNotFullySetUp)
    }

    @Test
    fun aDisabledAccountIsItsOwnCaseAndNotThisOne() {
        // Keycloak says "Account disabled" for a disabled user — a different
        // situation, and confirming an email would not help.
        assertFalse(LoginException(400, "invalid_grant", "Account disabled").isAccountNotFullySetUp)
    }

    @Test
    fun theMessageNeverCarriesCredentials() {
        val e = LoginException(400, "invalid_grant", "Account is not fully set up")
        val message = e.message.orEmpty()
        assertFalse(message.contains("password", ignoreCase = true), "message was $message")
    }
}
