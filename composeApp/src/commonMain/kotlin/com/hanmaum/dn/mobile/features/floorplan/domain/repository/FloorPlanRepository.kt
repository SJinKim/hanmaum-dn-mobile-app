package com.hanmaum.dn.mobile.features.floorplan.domain.repository

import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room

interface FloorPlanRepository {
    suspend fun getFloors(): List<Floor>
    suspend fun getRooms(floorId: String): List<Room>
}
