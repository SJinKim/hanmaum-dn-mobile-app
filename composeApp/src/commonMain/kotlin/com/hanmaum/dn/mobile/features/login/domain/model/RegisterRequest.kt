package com.hanmaum.dn.mobile.features.login.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    // PFLICHTFELDER (Müssen gefüllt sein)
    val firstName: String,
    val lastName: String,
    val email: String,
    val city: String,
    val password: String,

    // OPTIONALE FELDER (Können null sein)
    // Wenn null, regelt das Backend das (z.B. Enum parse error vermeiden)
    val baptism: String? = null,
    val gender: String? = null,
    val birthDate: String? = null, // Format: YYYY-MM-DD
    val phoneNumber: String? = null,
    val street: String? = null,
    @SerialName("zip_code") val zipCode: String? = null
)