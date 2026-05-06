package com.hanmaum.dn.mobile.features.floorplan.domain.model

data class Room(
    val id: String,
    val floorId: String,
    val name: String,
    val description: String,
    val points: List<Point>,
)
