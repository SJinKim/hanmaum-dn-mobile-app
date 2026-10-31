package com.hanmaum.dn.mobile.features.login.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    // PFLICHTFELDER (Müssen gefüllt sein)
    val firstName: String,
    val lastName: String,
    val email: String,
    val city: String,
    val password: String,
    val birthDate: String, // Format: YYYY-MM-DD — required since #282
    val identityStatus: String, // IdentityStatus name — required since #282

    val consent: RegistrationConsent,

    // OPTIONALE FELDER (Können null sein)
    // Wenn null, regelt das Backend das (z.B. Enum parse error vermeiden)
    val baptism: String? = null,
    val gender: String? = null,
    val phoneNumber: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val zipCode: String? = null,
    val workOrSchool: String? = null, // 소속 — school or employer, free text
)

/** The privacy acceptance that travels with the sign-up request. */
@Serializable
data class RegistrationConsent(val accepted: Boolean, val version: String)
