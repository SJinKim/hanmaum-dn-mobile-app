package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.model.MyRegistration
import com.hanmaum.dn.mobile.features.ministry.domain.model.RegistrationStatus
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository

/** Hand-written, like every other fake here — there is no mocking library. */
class FakeMinistryRepository : MinistryRepository {

    var ministriesResult: Result<List<Ministry>> = Result.success(emptyList())
    var detailResult: Result<MinistryDetail> = Result.success(detail())
    var registrationResult: Result<MyRegistration?> = Result.success(null)
    var registerResult: Result<MyRegistration> = Result.success(
        MyRegistration(publicId = "r1", status = RegistrationStatus.PENDING, note = null),
    )

    var lastActiveOnly: Boolean? = null
    var registerCalls = 0
    var lastRegisteredNote: String? = null
    var lastRegisteredMinistry: String? = null

    override suspend fun getMinistries(activeOnly: Boolean): Result<List<Ministry>> {
        lastActiveOnly = activeOnly
        return ministriesResult
    }

    override suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail> = detailResult

    override suspend fun getMyRegistration(ministryPublicId: String): Result<MyRegistration?> =
        registrationResult

    override suspend fun register(ministryPublicId: String, note: String?): Result<MyRegistration> {
        registerCalls++
        lastRegisteredMinistry = ministryPublicId
        lastRegisteredNote = note
        return registerResult
    }

    companion object {
        fun ministry(id: String = "m1", name: String = "난민 사역", active: Boolean = true) = Ministry(
            publicId = id,
            name = name,
            shortDescription = "돕는 손길",
            imageUrl = null,
            leaderName = "김승진",
            isActive = active,
        )

        fun detail(id: String = "m1", active: Boolean = true) = MinistryDetail(
            publicId = id,
            name = "난민 사역",
            shortDescription = "돕는 손길",
            longDescription = "매주 토요일에 모입니다.",
            imageUrl = null,
            leaderName = "김승진",
            isActive = active,
        )
    }
}
