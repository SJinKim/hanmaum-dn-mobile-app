# Floor Plan Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an interactive church floor plan screen (2 floors, rooms from backend) accessible from the Home screen via a map icon.

**Architecture:** New `features/floorplan/` feature following the existing clean-architecture pattern (domain → data → presentation). Room geometry is stored as normalized polygon point arrays (0–1 space) on the backend; the Canvas scales them to device size at render time. Point-in-polygon ray casting handles tap detection.

**Tech Stack:** Compose Multiplatform 1.10.0, Ktor 3.3.3 (MockEngine for tests), Koin 4.0.0, kotlinx-serialization-json, Material3 TabRow + ModalBottomSheet

---

## Backend Prerequisites

> ⚠️ These tables and endpoints must exist in the **backend service** (separate repo) before the mobile app can fetch real data. Implement these first or use a stub response for mobile development.

**Tables to create:**

```sql
CREATE TABLE floor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    floor_number INT NOT NULL,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE room (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    floor_id UUID NOT NULL REFERENCES floor(id),
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    points JSONB NOT NULL  -- [[x1,y1],[x2,y2],...] normalized 0.0–1.0
);
```

**Seed data:**
```sql
INSERT INTO floor (id, floor_number, name) VALUES
    ('11111111-0000-0000-0000-000000000001', 1, '1층'),
    ('11111111-0000-0000-0000-000000000002', 2, '2층');
```

**API endpoints (auth required, GET, JSON):**

```
GET /api/v1/floorplan/floors
→ [{"id":"...","floorNumber":1,"name":"1층"}, ...]

GET /api/v1/floorplan/floors/{floorId}/rooms
→ [{"id":"...","floorId":"...","name":"대예배실","description":"...","points":[[0.05,0.2],[0.9,0.2],[0.9,0.8],[0.05,0.8]]}, ...]
```

---

## File Map

### New Files

| File | Responsibility |
|---|---|
| `…/features/floorplan/domain/model/Floor.kt` | Plain domain model |
| `…/features/floorplan/domain/model/Point.kt` | Normalized coordinate + `List<Point>.hitTest()` |
| `…/features/floorplan/domain/model/Room.kt` | Plain domain model with `List<Point>` |
| `…/features/floorplan/domain/repository/FloorPlanRepository.kt` | Repository interface |
| `…/features/floorplan/data/model/FloorDto.kt` | Serializable DTO + `toDomain()` |
| `…/features/floorplan/data/model/RoomDto.kt` | Serializable DTO + `toDomain()` |
| `…/features/floorplan/data/repository/FloorPlanRepositoryImpl.kt` | Ktor implementation |
| `…/features/floorplan/presentation/FloorPlanUiState.kt` | Sealed UI state |
| `…/features/floorplan/presentation/FloorPlanViewModel.kt` | ViewModel |
| `…/features/floorplan/presentation/FloorPlanScreen.kt` | Root screen composable |
| `…/features/floorplan/presentation/components/FloorSelector.kt` | TabRow floor switcher |
| `…/features/floorplan/presentation/components/FloorCanvas.kt` | Canvas rendering + hit detection |
| `…/features/floorplan/presentation/components/RoomBottomSheet.kt` | ModalBottomSheet room info |
| `…/features/announcement/presentation/components/QuickAccessSection.kt` | Home screen shortcut row |
| `…/commonTest/…/features/floorplan/data/repository/FloorPlanRepositoryImplTest.kt` | Repository tests |
| `…/commonTest/…/features/floorplan/domain/model/PointHitTestTest.kt` | Hit detection tests |

### Modified Files

| File | Change |
|---|---|
| `…/core/navigation/Routes.kt` | Add `FloorPlanRoute` |
| `…/di/AppModule.kt` | Add repository + ViewModel bindings |
| `…/App.kt` | Register `composable<FloorPlanRoute>`, add `onFloorPlanClick` to HomeRoute |
| `…/features/announcement/presentation/HomeScreen.kt` | Add `onFloorPlanClick` param + `QuickAccessSection` |

---

## Base Paths (abbreviated in tasks below)

```
SRC = composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile
TEST = composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile
```

---

## Task 1: Domain Models + Hit Test + Tests

**Files:**
- Create: `$SRC/features/floorplan/domain/model/Floor.kt`
- Create: `$SRC/features/floorplan/domain/model/Point.kt`
- Create: `$SRC/features/floorplan/domain/model/Room.kt`
- Create: `$TEST/features/floorplan/domain/model/PointHitTestTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// $TEST/features/floorplan/domain/model/PointHitTestTest.kt
package com.hanmaum.dn.mobile.features.floorplan.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PointHitTestTest {

    private val square = listOf(
        Point(0f, 0f), Point(1f, 0f), Point(1f, 1f), Point(0f, 1f)
    )

    @Test
    fun hitTest_returnsTrueForPointInsideSquare() {
        assertTrue(square.hitTest(Point(0.5f, 0.5f)))
    }

    @Test
    fun hitTest_returnsFalseForPointOutsideSquare() {
        assertFalse(square.hitTest(Point(1.5f, 0.5f)))
    }

    @Test
    fun hitTest_returnsFalseForPointAboveSquare() {
        assertFalse(square.hitTest(Point(0.5f, -0.1f)))
    }

    @Test
    fun hitTest_returnsFalseForEmptyPolygon() {
        assertFalse(emptyList<Point>().hitTest(Point(0.5f, 0.5f)))
    }

    @Test
    fun hitTest_returnsTrueForPointInsideTriangle() {
        val triangle = listOf(Point(0f, 0f), Point(1f, 0f), Point(0.5f, 1f))
        assertTrue(triangle.hitTest(Point(0.5f, 0.4f)))
    }

    @Test
    fun hitTest_returnsFalseForPointOutsideTriangle() {
        val triangle = listOf(Point(0f, 0f), Point(1f, 0f), Point(0.5f, 1f))
        assertFalse(triangle.hitTest(Point(0.1f, 0.9f)))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.hanmaum.dn.mobile.features.floorplan.domain.model.PointHitTestTest"
```

Expected: FAIL — `Point` and `hitTest` are not defined.

- [ ] **Step 3: Create domain models**

```kotlin
// $SRC/features/floorplan/domain/model/Floor.kt
package com.hanmaum.dn.mobile.features.floorplan.domain.model

data class Floor(val id: String, val floorNumber: Int, val name: String)
```

```kotlin
// $SRC/features/floorplan/domain/model/Point.kt
package com.hanmaum.dn.mobile.features.floorplan.domain.model

data class Point(val x: Float, val y: Float)

fun List<Point>.hitTest(tap: Point): Boolean {
    if (isEmpty()) return false
    var inside = false
    var j = size - 1
    for (i in indices) {
        val xi = this[i].x; val yi = this[i].y
        val xj = this[j].x; val yj = this[j].y
        if ((yi > tap.y) != (yj > tap.y) &&
            tap.x < (xj - xi) * (tap.y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}
```

```kotlin
// $SRC/features/floorplan/domain/model/Room.kt
package com.hanmaum.dn.mobile.features.floorplan.domain.model

data class Room(
    val id: String,
    val floorId: String,
    val name: String,
    val description: String,
    val points: List<Point>,
)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.hanmaum.dn.mobile.features.floorplan.domain.model.PointHitTestTest"
```

Expected: 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git checkout -b feat/floor-plan
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/domain \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/floorplan/domain
git commit -m "feat(floorplan): add domain models and point-in-polygon hit detection"
```

---

## Task 2: Repository Interface + DTOs

**Files:**
- Create: `$SRC/features/floorplan/domain/repository/FloorPlanRepository.kt`
- Create: `$SRC/features/floorplan/data/model/FloorDto.kt`
- Create: `$SRC/features/floorplan/data/model/RoomDto.kt`

- [ ] **Step 1: Create repository interface**

```kotlin
// $SRC/features/floorplan/domain/repository/FloorPlanRepository.kt
package com.hanmaum.dn.mobile.features.floorplan.domain.repository

import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room

interface FloorPlanRepository {
    suspend fun getFloors(): List<Floor>
    suspend fun getRooms(floorId: String): List<Room>
}
```

- [ ] **Step 2: Create DTOs**

```kotlin
// $SRC/features/floorplan/data/model/FloorDto.kt
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
```

```kotlin
// $SRC/features/floorplan/data/model/RoomDto.kt
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
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/domain/repository \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/data/model
git commit -m "feat(floorplan): add repository interface and DTOs"
```

---

## Task 3: Repository Implementation + Tests

**Files:**
- Create: `$SRC/features/floorplan/data/repository/FloorPlanRepositoryImpl.kt`
- Create: `$TEST/features/floorplan/data/repository/FloorPlanRepositoryImplTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// $TEST/features/floorplan/data/repository/FloorPlanRepositoryImplTest.kt
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.hanmaum.dn.mobile.features.floorplan.data.repository.FloorPlanRepositoryImplTest"
```

Expected: FAIL — `FloorPlanRepositoryImpl` not defined.

- [ ] **Step 3: Implement the repository**

```kotlin
// $SRC/features/floorplan/data/repository/FloorPlanRepositoryImpl.kt
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
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.hanmaum.dn.mobile.features.floorplan.data.repository.FloorPlanRepositoryImplTest"
```

Expected: 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/data/repository \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/floorplan/data
git commit -m "feat(floorplan): add repository implementation with Ktor"
```

---

## Task 4: UiState + ViewModel

**Files:**
- Create: `$SRC/features/floorplan/presentation/FloorPlanUiState.kt`
- Create: `$SRC/features/floorplan/presentation/FloorPlanViewModel.kt`

- [ ] **Step 1: Create UiState**

```kotlin
// $SRC/features/floorplan/presentation/FloorPlanUiState.kt
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
```

- [ ] **Step 2: Create ViewModel**

```kotlin
// $SRC/features/floorplan/presentation/FloorPlanViewModel.kt
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
                _uiState.value = (_uiState.value as? FloorPlanUiState.Success)
                    ?.copy(rooms = rooms) ?: _uiState.value
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
```

- [ ] **Step 3: Build to verify compilation**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanUiState.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanViewModel.kt
git commit -m "feat(floorplan): add UiState and ViewModel"
```

---

## Task 5: FloorSelector Component

**Files:**
- Create: `$SRC/features/floorplan/presentation/components/FloorSelector.kt`

- [ ] **Step 1: Create component**

```kotlin
// $SRC/features/floorplan/presentation/components/FloorSelector.kt
package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor

@Composable
fun FloorSelector(
    floors: List<Floor>,
    selectedFloor: Floor,
    onFloorSelected: (Floor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = floors.indexOfFirst { it.id == selectedFloor.id }.coerceAtLeast(0)
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        floors.forEach { floor ->
            Tab(
                selected = floor.id == selectedFloor.id,
                onClick = { onFloorSelected(floor) },
                text = { Text(floor.name, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorSelector.kt
git commit -m "feat(floorplan): add FloorSelector tab component"
```

---

## Task 6: FloorCanvas Component

**Files:**
- Create: `$SRC/features/floorplan/presentation/components/FloorCanvas.kt`

- [ ] **Step 1: Create component**

```kotlin
// $SRC/features/floorplan/presentation/components/FloorCanvas.kt
package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Point
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import com.hanmaum.dn.mobile.features.floorplan.domain.model.hitTest

@Composable
fun FloorCanvas(
    rooms: List<Room>,
    selectedRoom: Room?,
    onRoomTap: (Room) -> Unit,
    onEmptyTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .pointerInput(rooms, selectedRoom) {
                detectTapGestures { tapOffset ->
                    val normalizedTap = Point(
                        x = tapOffset.x / size.width,
                        y = tapOffset.y / size.height,
                    )
                    val hit = rooms.firstOrNull { it.points.hitTest(normalizedTap) }
                    if (hit != null) onRoomTap(hit) else onEmptyTap()
                }
            },
    ) {
        rooms.forEach { room ->
            val scaled = room.points.map { Offset(it.x * size.width, it.y * size.height) }
            if (scaled.isEmpty()) return@forEach

            val path = Path().apply {
                moveTo(scaled.first().x, scaled.first().y)
                scaled.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }

            val fillColor = if (room.id == selectedRoom?.id) primaryContainer else surfaceContainerLow
            drawPath(path, color = fillColor, style = Fill)
            drawPath(path, color = onSurface.copy(alpha = 0.15f), style = Stroke(width = 1.dp.toPx()))

            val cx = room.points.map { it.x }.average().toFloat() * size.width
            val cy = room.points.map { it.y }.average().toFloat() * size.height
            val measured = textMeasurer.measure(
                text = room.name,
                style = TextStyle(fontSize = 12.sp, color = onSurface),
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(cx - measured.size.width / 2f, cy - measured.size.height / 2f),
            )
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorCanvas.kt
git commit -m "feat(floorplan): add FloorCanvas with polygon rendering and tap detection"
```

---

## Task 7: RoomBottomSheet Component

**Files:**
- Create: `$SRC/features/floorplan/presentation/components/RoomBottomSheet.kt`

- [ ] **Step 1: Create component**

```kotlin
// $SRC/features/floorplan/presentation/components/RoomBottomSheet.kt
package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomBottomSheet(
    room: Room,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text(
                text = room.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = room.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/RoomBottomSheet.kt
git commit -m "feat(floorplan): add RoomBottomSheet component"
```

---

## Task 8: FloorPlanScreen

**Files:**
- Create: `$SRC/features/floorplan/presentation/FloorPlanScreen.kt`

- [ ] **Step 1: Create screen**

```kotlin
// $SRC/features/floorplan/presentation/FloorPlanScreen.kt
package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorCanvas
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorSelector
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.RoomBottomSheet
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: FloorPlanViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "교회 지도",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is FloorPlanUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FloorPlanUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is FloorPlanUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FloorSelector(
                            floors = state.floors,
                            selectedFloor = state.selectedFloor,
                            onFloorSelected = viewModel::selectFloor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FloorCanvas(
                            rooms = state.rooms,
                            selectedRoom = state.selectedRoom,
                            onRoomTap = viewModel::selectRoom,
                            onEmptyTap = viewModel::clearSelectedRoom,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    state.selectedRoom?.let { room ->
                        RoomBottomSheet(
                            room = room,
                            onDismiss = viewModel::clearSelectedRoom,
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt
git commit -m "feat(floorplan): add FloorPlanScreen composable"
```

---

## Task 9: Navigation Route + DI + App.kt Wiring

**Files:**
- Modify: `$SRC/core/navigation/Routes.kt`
- Modify: `$SRC/di/AppModule.kt`
- Modify: `$SRC/App.kt`

- [ ] **Step 1: Add FloorPlanRoute to Routes.kt**

In `$SRC/core/navigation/Routes.kt`, add at the end:

```kotlin
@Serializable object FloorPlanRoute
```

Full file after change:
```kotlin
package com.hanmaum.dn.mobile.core.navigation

import kotlinx.serialization.Serializable

@Serializable object SplashRoute
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object PendingRoute
@Serializable object HomeRoute
@Serializable object AnnouncementListRoute
@Serializable data class AnnouncementDetailRoute(val id: String)
@Serializable object ProfileRoute
@Serializable object MinistryListRoute
@Serializable data class MinistryDetailRoute(val publicId: String)
@Serializable object CommunityRoute
@Serializable object FloorPlanRoute
```

- [ ] **Step 2: Add DI bindings to AppModule.kt**

In `$SRC/di/AppModule.kt`, add the following two lines inside the `module { }` block after the existing ministry bindings:

```kotlin
// FloorPlan
single<FloorPlanRepository> { FloorPlanRepositoryImpl(get()) }
viewModel { FloorPlanViewModel(get()) }
```

Also add these imports at the top of `AppModule.kt`:
```kotlin
import com.hanmaum.dn.mobile.features.floorplan.data.repository.FloorPlanRepositoryImpl
import com.hanmaum.dn.mobile.features.floorplan.domain.repository.FloorPlanRepository
import com.hanmaum.dn.mobile.features.floorplan.presentation.FloorPlanViewModel
```

- [ ] **Step 3: Register composable in App.kt**

In `$SRC/App.kt`, add the import:
```kotlin
import com.hanmaum.dn.mobile.features.floorplan.presentation.FloorPlanScreen
import com.hanmaum.dn.mobile.core.navigation.FloorPlanRoute
```

Then update the `HomeRoute` composable to pass the new callback:
```kotlin
composable<HomeRoute> {
    HomeScreen(
        onAnnouncementClick = { id ->
            navController.navigate(AnnouncementDetailRoute(id = id))
        },
        onViewAllClick = { navController.navigate(AnnouncementListRoute) },
        onFloorPlanClick = { navController.navigate(FloorPlanRoute) },
    )
}
```

Then add the new composable after `CommunityRoute`:
```kotlin
composable<FloorPlanRoute> {
    FloorPlanScreen(
        onBackClick = { navController.popBackStack() },
    )
}
```

- [ ] **Step 4: Build to verify compilation**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL (will show `HomeScreen` missing `onFloorPlanClick` param — that's fixed in Task 10).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/Routes.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt
git commit -m "feat(floorplan): wire FloorPlanRoute into navigation and Koin DI"
```

---

## Task 10: HomeScreen Shortcut + Final Verification

**Files:**
- Create: `$SRC/features/announcement/presentation/components/QuickAccessSection.kt`
- Modify: `$SRC/features/announcement/presentation/HomeScreen.kt`

- [ ] **Step 1: Create QuickAccessSection component**

```kotlin
// $SRC/features/announcement/presentation/components/QuickAccessSection.kt
package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickAccessSection(
    onFloorPlanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "빠른 메뉴",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .clickable { onFloorPlanClick() }
                    .padding(0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "교회 지도",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "교회 지도",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update HomeScreen.kt**

In `$SRC/features/announcement/presentation/HomeScreen.kt`:

**a) Add `onFloorPlanClick` to the `HomeScreen` signature:**
```kotlin
@Composable
fun HomeScreen(
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onFloorPlanClick: () -> Unit,
)
```

**b) Pass `onFloorPlanClick` to `HomeContent`:**
```kotlin
else -> HomeContent(
    state               = state,
    attendanceState     = attendanceState,
    onAnnouncementClick = onAnnouncementClick,
    onViewAllClick      = onViewAllClick,
    onCheckIn           = attendanceViewModel::checkIn,
    onFloorPlanClick    = onFloorPlanClick,
)
```

**c) Add `onFloorPlanClick` to `HomeContent` signature:**
```kotlin
@Composable
private fun HomeContent(
    state: HomeUiState,
    attendanceState: AttendanceUiState,
    onAnnouncementClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onCheckIn: () -> Unit,
    onFloorPlanClick: () -> Unit,
)
```

**d) Add `QuickAccessSection` below `LatestNewsSection` inside `HomeContent`:**
```kotlin
LatestNewsSection(
    newsList       = state.announcements,
    onItemClick    = onAnnouncementClick,
    onViewAllClick = onViewAllClick,
)

Spacer(modifier = Modifier.height(24.dp))

QuickAccessSection(
    onFloorPlanClick = onFloorPlanClick,
)

Spacer(modifier = Modifier.height(32.dp))
```

**e) Add the import at the top of `HomeScreen.kt`:**
```kotlin
import com.hanmaum.dn.mobile.features.announcement.presentation.components.QuickAccessSection
```

- [ ] **Step 3: Build to verify compilation**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all tests**

```bash
./gradlew :composeApp:allTests
```

Expected: All tests PASS including the 12 new floorplan tests.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/components/QuickAccessSection.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/HomeScreen.kt
git commit -m "feat(floorplan): add map shortcut to HomeScreen quick access row"
```

- [ ] **Step 6: Open PR**

```bash
git push -u origin feat/floor-plan
```

Then open a PR from `feat/floor-plan` → `main`.
