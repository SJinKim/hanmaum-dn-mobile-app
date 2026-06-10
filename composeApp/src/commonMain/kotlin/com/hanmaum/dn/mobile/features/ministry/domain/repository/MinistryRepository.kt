package com.hanmaum.dn.mobile.features.ministry.domain.repository

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail

interface MinistryRepository {
    suspend fun getMinistries(activeOnly: Boolean = true): Result<List<Ministry>>
    suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail>
}
