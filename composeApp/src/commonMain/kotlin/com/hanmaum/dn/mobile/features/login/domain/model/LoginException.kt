package com.hanmaum.dn.mobile.features.login.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Keycloak's error body for a refused token request. */
@Serializable
data class KeycloakError(
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

/**
 * Keycloak refused the password grant, with whatever it said about why.
 *
 * A plain Exception carrying a formatted string was enough while every refusal
 * was treated the same. It is not enough once one of them means something the
 * member can act on: a pending required action such as VERIFY_EMAIL refuses
 * every login until the confirmation link is clicked, and telling that member
 * to "please log in" sends them at a door that will not open (#168).
 */
class LoginException(
    val status: Int,
    val error: String?,
    val description: String?,
) : Exception("Login refused ($status): $error — $description") {

    /**
     * Keycloak has a required action pending on the account — in this app
     * that means the email is not confirmed yet.
     *
     * Matched on Keycloak's own wording rather than a status code, because
     * `invalid_grant` also covers a plain wrong password. Deliberately
     * case-insensitive and substring-based: the exact phrasing is not part of
     * any contract, so a stricter match would fail silently on an upgrade and
     * fall back to the generic message, which is the safe direction.
     */
    val isAccountNotFullySetUp: Boolean
        get() = error == "invalid_grant" &&
            description?.contains("not fully set up", ignoreCase = true) == true
}
