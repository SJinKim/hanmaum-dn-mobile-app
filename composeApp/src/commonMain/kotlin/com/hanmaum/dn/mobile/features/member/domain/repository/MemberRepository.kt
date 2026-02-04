package com.hanmaum.dn.mobile.features.member.domain.repository

import com.hanmaum.dn.mobile.core.domain.model.Member

interface MemberRepository {
    suspend fun getMyProfile(): Result<Member>
}