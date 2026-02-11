package com.hanmaum.dn.mobile.features.member.data.model

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import kotlinx.serialization.Serializable

@Serializable
data class MemberResponse (
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val role: String,
    val groupName: String? = null,
    val city: String,
    val status: MemberStatus
)

