# Floor Plan Feature — Design Spec
**Date:** 2026-05-07  
**Status:** Approved

---

## Overview

An interactive church floor plan screen that lets users find rooms inside the building. Accessible from the Home screen via a map icon shortcut below the news section. Supports 2 floors with a floor selector. Room shapes and metadata are served from the backend.

---

## Entry Point

A quick-access shortcut row is added to `HomeScreen` below the latest news section. It contains a map icon + "교회 지도" label. Tapping navigates to `FloorPlanRoute`.

No ViewModel changes to `HomeScreen` — navigation is a pure UI action.

---

## Backend

### Database Tables

**`floor`**
| column | type | notes | 
|---|---|---|
| `id` | UUID | PK |
| `floor_number` | INT | 1, 2, ... |
| `name` | VARCHAR | e.g. "1층", "2층" |

**`room`**
| column | type | notes |
|---|---|---|
| `id` | UUID | PK |
| `floor_id` | UUID | FK → floor |
| `name` | VARCHAR | e.g. "대예배실" |
| `description` | TEXT | admin-editable, flexible length |
| `points` | JSONB | `[[x1,y1],[x2,y2],...]` normalized 0.0–1.0 |

`points` are stored as normalized coordinates (0.0–1.0) relative to the canvas dimensions. This allows the floor plan to scale correctly to any screen size without zoom/pan.

### API Endpoints

Both endpoints require authentication. Read-only.

```
GET /api/v1/floorplan/floors
Response: [{ id, floorNumber, name }]

GET /api/v1/floorplan/floors/{floorId}/rooms
Response: [{ id, floorId, name, description, points: [[x,y],...] }]
```

---

## Frontend (KMP / Compose Multiplatform)

### Feature Structure

```
features/floorplan/
  domain/
    model/
      Floor.kt
      Room.kt
    repository/
      FloorPlanRepository.kt
  data/
    model/
      FloorDto.kt
      RoomDto.kt
    repository/
      FloorPlanRepositoryImpl.kt
  presentation/
    FloorPlanViewModel.kt
    FloorPlanUiState.kt
    FloorPlanScreen.kt
    components/
      FloorSelector.kt
      FloorCanvas.kt
      RoomBottomSheet.kt
```

### Domain Models

```kotlin
data class Floor(val id: String, val floorNumber: Int, val name: String)

data class Point(val x: Float, val y: Float)  // normalized 0f–1f, no Compose dependency

data class Room(
    val id: String,
    val floorId: String,
    val name: String,
    val description: String,
    val points: List<Point>,
)
```

### UI State

```kotlin
sealed class FloorPlanUiState {
    object Loading : FloorPlanUiState()
    data class Success(
        val floors: List<Floor>,
        val selectedFloor: Floor,
        val rooms: List<Room>,
        val selectedRoom: Room?,
    ) : FloorPlanUiState()
    data class Error(val message: String) : FloorPlanUiState()
}
```

### ViewModel Behaviour

- On init: fetch all floors, auto-select floor 1, then fetch rooms for floor 1.
- On floor tab change: fetch rooms for the newly selected floor, clear `selectedRoom`.
- On canvas tap: run point-in-polygon hit detection, set `selectedRoom` (triggers bottom sheet).
- On bottom sheet dismiss: clear `selectedRoom`.

### Canvas Rendering (FloorCanvas)

1. Receive `rooms: List<Room>` and `selectedRoomId: String?`.
2. For each room, build a `Path` by converting `Point(x,y)` → `Offset(x * size.width, y * size.height)`.
3. Draw fill: unselected → `MaterialTheme.colorScheme.surfaceContainerLow`, selected → `MaterialTheme.colorScheme.primaryContainer`.
4. Draw room name label at polygon centroid using `drawContext.canvas.nativeCanvas` (or `drawText`).
5. Canvas background: `surfaceContainerLow` (#f3f3f3).

### Hit Detection — Point-in-Polygon (Ray Casting)

On tap, the raw `Offset` is normalized (divide by canvas size) to a `Point(0–1, 0–1)`, then tested against each room's `points` list using ray casting:

```kotlin
fun List<Point>.contains(tap: Point): Boolean {
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

First match wins. If no match, `selectedRoom` is cleared.

### Screen Layout

```
┌─────────────────────────────┐
│  ← TopBar: "교회 지도"       │
├─────────────────────────────┤
│  [ 1층 ]  [ 2층 ]           │  ← FloorSelector (TabRow, secondary indicator)
├─────────────────────────────┤
│                             │
│       FloorCanvas           │  ← fillMaxSize
│    (rooms drawn here)       │
│                             │
└─────────────────────────────┘
```

On room tap, a `ModalBottomSheet` appears:
- Room name: `Headline` typography
- Description: `Body` typography, `onSurfaceVariant` color
- Dismiss on swipe or tap outside

### Navigation

- Add `@Serializable object FloorPlanRoute` to `Routes.kt`
- Register `composable<FloorPlanRoute>` in `App.kt`
- `FloorPlanRoute` is NOT a `TopLevelDestination` — it is a secondary screen with a back button

### Design System Compliance

- No divider lines — use `surfaceContainerLow` canvas background for separation
- `rounded-xl` for bottom sheet handle area
- Colors from Material3 tokens only (`primaryContainer`, `surfaceContainerLow`, `onSurfaceVariant`)
- Typography: Plus Jakarta Sans via existing `AppTheme`

---

## Dependency Injection

Add to `AppModule.kt`:
```kotlin
single<FloorPlanRepository> { FloorPlanRepositoryImpl(get()) }
viewModel { FloorPlanViewModel(get()) }
```

---

## Out of Scope (v1)

- Pinch-to-zoom / pan
- Admin UI for editing rooms
- Room search / filtering
- Directions / wayfinding
