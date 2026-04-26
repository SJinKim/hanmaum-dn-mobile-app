package com.hanmaum.dn.mobile.features.geofence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChurchLocationResponse(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)
