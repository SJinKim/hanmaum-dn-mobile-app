package com.hanmaum.dn.mobile.features.login.domain.model

/** Pure, platform-free field validation for the registration form. */
object RegisterValidation {
    // Pragmatic email shape check: one @, no spaces, a dot in the domain.
    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    // Per-country postcode shapes. Default is Germany; add entries to extend.
    // We validate the *shape*, not existence (avoids false rejections — Baymard).
    private val POSTAL_REGEX: Map<String, Regex> = mapOf(
        "DE" to Regex("^\\d{5}$"),
        "AT" to Regex("^\\d{4}$"),
        "CH" to Regex("^\\d{4}$"),
        "KR" to Regex("^\\d{5}$"),
        "US" to Regex("^\\d{5}(-\\d{4})?$"),
        "GB" to Regex("^[A-Za-z]{1,2}\\d[A-Za-z\\d]?\\s*\\d[A-Za-z]{2}$"),
        "FR" to Regex("^\\d{5}$"),
        "NL" to Regex("^\\d{4}\\s*[A-Za-z]{2}$"),
    )
    private val DEFAULT_POSTAL = Regex("^[A-Za-z\\d][A-Za-z\\d\\s-]{1,9}$") // lenient fallback

    fun isValidPostalCode(code: String, countryIso: String = "DE"): Boolean {
        val regex = POSTAL_REGEX[countryIso.uppercase()] ?: DEFAULT_POSTAL
        return regex.matches(code.trim())
    }

    /** A city must contain at least one letter (rejects digits-only input). */
    fun isValidCity(city: String): Boolean = city.trim().any { it.isLetter() }

    /** A house number must contain at least one digit (e.g. "5", "12a", "12-14"). */
    fun isValidHouseNumber(houseNumber: String): Boolean = houseNumber.any { it.isDigit() }
}

/**
 * Raised when registration fails on the backend. [userMessage] is a clean,
 * user-facing message extracted from the API response when available — never a
 * raw status dump. A null/blank message means "show a generic localized error".
 */
class RegisterException(val userMessage: String?) : Exception(userMessage)
