package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import com.hanmaum.dn.mobile.features.floorplan.domain.repository.FloorPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloorPlanViewModel(
    private val repository: FloorPlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FloorPlanUiState>(FloorPlanUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadFloors()
    }

    private fun loadFloors() {
        viewModelScope.launch {
            try {
                val floors = repository.getFloors()
                if (floors.isEmpty()) {
                    _uiState.value = FloorPlanUiState.Error("등록된 층 정보가 없습니다.")
                    return@launch
                }
                val first = floors.first()
                val rooms = repository.getRooms(first.id)
                _uiState.value = FloorPlanUiState.Success(
                    floors = floors,
                    selectedFloor = first,
                    rooms = rooms,
                    selectedRoom = null,
                )
            } catch (e: Exception) {
                _uiState.value = FloorPlanUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
            }
        }
    }

    fun selectFloor(floor: Floor) {
        val current = _uiState.value as? FloorPlanUiState.Success ?: return
        _uiState.value = current.copy(selectedFloor = floor, rooms = emptyList(), selectedRoom = null)
        viewModelScope.launch {
            try {
                val rooms = repository.getRooms(floor.id)
                val latest = _uiState.value as? FloorPlanUiState.Success ?: return@launch
                if (latest.selectedFloor.id == floor.id) {
                    _uiState.value = latest.copy(rooms = rooms)
                }
            } catch (e: Exception) {
                _uiState.value = FloorPlanUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
            }
        }
    }

    fun selectRoom(room: Room) {
        val current = _uiState.value as? FloorPlanUiState.Success ?: return
        _uiState.value = current.copy(selectedRoom = room)
    }

    fun clearSelectedRoom() {
        val current = _uiState.value as? FloorPlanUiState.Success ?: return
        _uiState.value = current.copy(selectedRoom = null)
    }
}
