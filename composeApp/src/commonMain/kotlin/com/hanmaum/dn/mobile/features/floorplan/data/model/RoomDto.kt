package com.hanmaum.dn.mobile.features.floorplan.data.model

import com.hanmaum.dn.mobile.features.floorplan.domain.model.Point
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import kotlinx.serialization.Serializable

@Serializable
data class RoomDto(
    val id: String,
    val floorId: String,
    val name: String,
    val description: String,
    val points: List<List<Float>>,
) {
    fun toDomain() = Room(
        id = id,
        floorId = floorId,
        name = name,
        description = description,
        points = points.map { Point(it[0], it[1]) },
    )
}
