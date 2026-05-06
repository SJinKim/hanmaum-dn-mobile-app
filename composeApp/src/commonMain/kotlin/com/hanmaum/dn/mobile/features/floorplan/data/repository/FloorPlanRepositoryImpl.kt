package com.hanmaum.dn.mobile.features.floorplan.data.repository

import com.hanmaum.dn.mobile.features.floorplan.data.model.FloorDto
import com.hanmaum.dn.mobile.features.floorplan.data.model.RoomDto
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import com.hanmaum.dn.mobile.features.floorplan.domain.repository.FloorPlanRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class FloorPlanRepositoryImpl(
    private val client: HttpClient,
) : FloorPlanRepository {

    override suspend fun getFloors(): List<Floor> = try {
        val response = client.get("floorplan/floors")
        if (response.status != HttpStatusCode.OK) emptyList()
        else response.body<List<FloorDto>>().map { it.toDomain() }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun getRooms(floorId: String): List<Room> = try {
        val response = client.get("floorplan/floors/$floorId/rooms")
        if (response.status != HttpStatusCode.OK) emptyList()
        else response.body<List<RoomDto>>().map { it.toDomain() }
    } catch (_: Exception) {
        emptyList()
    }
}
