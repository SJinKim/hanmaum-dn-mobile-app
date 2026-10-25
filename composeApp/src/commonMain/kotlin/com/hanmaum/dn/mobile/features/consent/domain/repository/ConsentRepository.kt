package com.hanmaum.dn.mobile.features.consent.domain.repository

import com.hanmaum.dn.mobile.features.consent.domain.model.*

interface ConsentRepository {
    suspend fun policies(): Result<List<ConsentPolicy>>
    suspend fun myConsents(): Result<MemberConsents>
    suspend fun grant(purpose: ConsentPurpose, version: String): Result<MemberConsent>
    suspend fun withdraw(purpose: ConsentPurpose): Result<Unit>
}

/** Session identity is for invalidation, not an authorization decision. */
interface ConsentSessionRepository {
    fun scope(): String?
    suspend fun logout(forgetBiometric: Boolean = false)
    suspend fun startLocation()
    suspend fun stopLocation()
}
