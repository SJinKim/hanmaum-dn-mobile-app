package com.hanmaum.dn.mobile.features.floorplan.data.repository

import com.hanmaum.dn.mobile.features.floorplan.data.model.FloorDto
import com.hanmaum.dn.mobile.features.floorplan.data.model.RoomDto
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Point
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    responseJson: String,
    onRequest: ((HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

class FloorPlanRepositoryImplTest {

    private val floor1 = FloorDto(id = "f1", floorNumber = 1, name = "1층")
    private val floor2 = FloorDto(id = "f2", floorNumber = 2, name = "2층")
    private val room1 = RoomDto(
        id = "r1", floorId = "f1", name = "대예배실", description = "주일 예배 공간",
        points = listOf(listOf(0.05f, 0.2f), listOf(0.9f, 0.2f), listOf(0.9f, 0.8f), listOf(0.05f, 0.8f)),
    )

    @Test
    fun getFloors_returnsDeserializedList() = runTest {
        val json = testJson.encodeToString(ListSerializer(FloorDto.serializer()), listOf(floor1, floor2))
        val result = FloorPlanRepositoryImpl(mockClient(json)).getFloors()

        assertEquals(2, result.size)
        assertEquals(Floor("f1", 1, "1층"), result[0])
        assertEquals(Floor("f2", 2, "2층"), result[1])
    }

    @Test
    fun getFloors_requestsCorrectPath() = runTest {
        var capturedPath = ""
        val json = testJson.encodeToString(ListSerializer(FloorDto.serializer()), emptyList())
        FloorPlanRepositoryImpl(mockClient(json) { capturedPath = it.url.encodedPath }).getFloors()

        assertEquals("/floorplan/floors", capturedPath)
    }

    @Test
    fun getFloors_returnsEmptyListOnEmptyResponse() = runTest {
        val result = FloorPlanRepositoryImpl(mockClient("[]")).getFloors()
        assertEquals(0, result.size)
    }

    @Test
    fun getRooms_returnsDeserializedListWithMappedPoints() = runTest {
        val json = testJson.encodeToString(ListSerializer(RoomDto.serializer()), listOf(room1))
        val result = FloorPlanRepositoryImpl(mockClient(json)).getRooms("f1")

        assertEquals(1, result.size)
        val room = result[0]
        assertEquals(Room(
            id = "r1", floorId = "f1", name = "대예배실", description = "주일 예배 공간",
            points = listOf(Point(0.05f, 0.2f), Point(0.9f, 0.2f), Point(0.9f, 0.8f), Point(0.05f, 0.8f)),
        ), room)
    }

    @Test
    fun getRooms_requestsCorrectPathWithFloorId() = runTest {
        var capturedPath = ""
        FloorPlanRepositoryImpl(mockClient("[]") { capturedPath = it.url.encodedPath }).getRooms("f1")

        assertEquals("/floorplan/floors/f1/rooms", capturedPath)
    }

    @Test
    fun getRooms_returnsEmptyListOnEmptyResponse() = runTest {
        val result = FloorPlanRepositoryImpl(mockClient("[]")).getRooms("f1")
        assertEquals(0, result.size)
    }
}
