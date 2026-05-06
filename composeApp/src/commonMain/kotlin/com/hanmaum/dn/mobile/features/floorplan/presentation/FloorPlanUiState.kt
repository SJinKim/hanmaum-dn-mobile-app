package com.hanmaum.dn.mobile.features.floorplan.presentation

import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room

sealed class FloorPlanUiState {
    data object Loading : FloorPlanUiState()
    data class Error(val message: String) : FloorPlanUiState()
    data class Success(
        val floors: List<Floor>,
        val selectedFloor: Floor,
        val rooms: List<Room>,
        val selectedRoom: Room?,
    ) : FloorPlanUiState()
}
