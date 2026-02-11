package com.hanmaum.dn.mobile.features.member.domain.repository

import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse

interface MemberRepository {
    suspend fun getMyProfile(): Result<MemberResponse>
}