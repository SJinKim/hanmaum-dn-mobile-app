package com.hanmaum.dn.mobile.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MemberStatus {
    ACTIVE,
    PENDING,
    REJECTED,
    DELETED
}