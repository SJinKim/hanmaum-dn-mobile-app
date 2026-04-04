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
    val city: String? = null,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
)
