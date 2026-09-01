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
    val division: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val zipCode: String? = null,
    val city: String? = null,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val birthDate: String? = null,
    /**
     * ISO date the member joined, feeding the profile's "함께한 시간" tile.
     * Nullable because it is nullable on the server too (an older record may
     * have none) — unlike the ministry isActive case (#129), the null here is
     * real data rather than a default papering over a wire-name mismatch.
     */
    val registrationDate: String? = null,
)
