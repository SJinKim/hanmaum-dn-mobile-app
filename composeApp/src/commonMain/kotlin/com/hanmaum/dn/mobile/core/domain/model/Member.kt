package com.hanmaum.dn.mobile.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,

    val city: String,
    val role: String,
    val groupName: String? = null,
    val status: String
)
