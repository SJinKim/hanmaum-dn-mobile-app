package com.hanmaum.dn.mobile.features.member.data.model

import com.hanmaum.dn.mobile.core.domain.model.MemberStatus
import kotlinx.serialization.Serializable

@Serializable
data class MemberResponse(
    val publicId: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val status: MemberStatus,
    val churchRole: String? = null,
    val groupName: String? = null,
    // already returned by GET /api/v1/members/me — the client simply never read them
    val division: String? = null,
    val houseNumber: String? = null,
    val birthDate: String? = null,
    val street: String? = null,
    @kotlinx.serialization.SerialName("zip_code") val zipCode: String? = null,
    val city: String? = null,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
)
