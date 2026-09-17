package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository

/** Hand-written, like every other fake here — there is no mocking library. */
class FakeMinistryRepository : MinistryRepository {

    var ministriesResult: Result<List<Ministry>> = Result.success(emptyList())
    var detailResult: Result<MinistryDetail> = Result.success(detail())

    var lastActiveOnly: Boolean? = null
    var detailCalls = 0
    var lastDetailId: String? = null

    override suspend fun getMinistries(activeOnly: Boolean): Result<List<Ministry>> {
        lastActiveOnly = activeOnly
        return ministriesResult
    }

    override suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail> {
        detailCalls++
        lastDetailId = publicId
        return detailResult
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
