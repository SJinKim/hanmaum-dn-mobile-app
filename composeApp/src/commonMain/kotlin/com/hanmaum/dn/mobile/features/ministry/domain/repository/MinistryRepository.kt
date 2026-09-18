package com.hanmaum.dn.mobile.features.ministry.domain.repository

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail

/**
 * Reading 사역 only.
 *
 * Self-registration used to live here too, against `ministries/{id}/registrations` — an
 * endpoint the server never had (#248). It is gone rather than dormant: when
 * hanmaum-dn-server#170 builds the real thing it brings its own contract (a separate
 * `ministry_applications` table, an approval step, its own states), so nothing here would
 * have fitted it anyway.
 */
interface MinistryRepository {
    suspend fun getMinistries(activeOnly: Boolean = true): Result<List<Ministry>>
    suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail>
}
