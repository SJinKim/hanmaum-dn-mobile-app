package com.hanmaum.dn.mobile.features.login.domain.model

/**
 * Result of evaluating a password against the registration policy.
 * Mirrors the Keycloak password policy so the UI checklist never shows a
 * green check for a rule the backend would reject.
 */
data class PasswordCriteria(
    val minLength: Boolean = false,
    val hasUpperAndLower: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecial: Boolean = false,
    val notEmail: Boolean = false,
) {
    val allMet: Boolean
        get() = minLength && hasUpperAndLower && hasDigit && hasSpecial && notEmail
}

/** Pure evaluation of password rules — no Compose, no platform dependency. */
object PasswordPolicy {
    const val MIN_LENGTH = 8

    fun evaluate(password: String, email: String): PasswordCriteria {
        if (password.isEmpty()) return PasswordCriteria()
        return PasswordCriteria(
            minLength = password.length >= MIN_LENGTH,
            hasUpperAndLower = password.any { it.isUpperCase() } && password.any { it.isLowerCase() },
            hasDigit = password.any { it.isDigit() },
            hasSpecial = password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
            // Keycloak notUsername/notEmail: the email is the username for this app.
            notEmail = !password.equals(email, ignoreCase = true),
        )
    }
}
