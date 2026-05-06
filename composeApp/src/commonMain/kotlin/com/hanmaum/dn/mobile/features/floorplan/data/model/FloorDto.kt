package com.hanmaum.dn.mobile.features.floorplan.data.model

import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import kotlinx.serialization.Serializable

@Serializable
data class FloorDto(
    val id: String,
    val floorNumber: Int,
    val name: String,
) {
    fun toDomain() = Floor(id = id, floorNumber = floorNumber, name = name)
}
