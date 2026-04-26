package com.hanmaum.dn.mobile.features.geofence.domain.repository

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation

interface ChurchLocationRepository {
    suspend fun getChurchLocation(): Result<ChurchLocation>
}
