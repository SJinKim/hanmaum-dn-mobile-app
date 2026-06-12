# Floor Plan UI Improvements — Design Spec

**Date:** 2026-05-08
**Feature:** `features/floorplan`
**Scope:** UI-only changes. No ViewModel, repository, or backend changes required.

---

## Problem

The floor plan screen uses half the portrait screen height for the map because the floor selector sits above the canvas in a `Column` stack. Additional issues: no zoom/pan, no way to discover rooms by name, and room tap opens a full bottom sheet that hides the map entirely.

---

## Goals

1. Give the canvas full screen real estate.
2. Surface room info without hiding the map.
3. Make small rooms reachable via pinch-to-zoom.
4. Support room discovery by name (list view).

---

## Out of Scope

- Backend changes
- ViewModel changes
- Navigation changes
- Animations beyond slide-in/out for the peek card

---

## Design

### 1. Full-Screen Canvas + Floating Floor Selector

**Files changed:** `FloorPlanScreen.kt`, `FloorSelector.kt`

**`FloorPlanScreen.kt`**
- Replace `Column { FloorSelector + FloorCanvas }` with `Box`.
- `FloorCanvas` modifier: `Modifier.fillMaxSize()`.
- `FloorSelector` modifier: `Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp)`.
- Remove the `verticalScroll` wrapper (was a workaround for the half-screen issue).

**`FloorSelector.kt`**
- Replace `TabRow` with a `Row` of `FilterChip`s.
- Wrap in `Surface(shape = CircleShape, tonalElevation = 4.dp)` for the floating pill card.
- Selected chip uses `selected = true` state; unselected chips use `selected = false`.
- The `Surface` gives a subtle tonal lift against the canvas background without a hard border (consistent with the "No-Line Rule" in DESIGN.md).

---

### 2. Peek Card + Expand to Full Sheet

**Files changed:** `FloorPlanScreen.kt`
**New file:** `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/RoomPeekCard.kt`
**Unchanged:** `RoomBottomSheet.kt`

**`RoomPeekCard.kt`**
- Composable signature: `RoomPeekCard(room: Room, onDismiss: () -> Unit, onExpand: () -> Unit, modifier: Modifier)`
- Layout: `Card(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))` filling full width.
- Content row: room name (`titleMedium`) + description excerpt (`bodySmall`, `maxLines = 2`, `overflow = Ellipsis`) + `IconButton(Icons.Default.KeyboardArrowUp)` calling `onExpand`.
- Internal padding: `16.dp` horizontal, `12.dp` vertical.

**`FloorPlanScreen.kt`** (Success state)
- Add local `var showFullSheet by remember { mutableStateOf(false) }`.
- Add `LaunchedEffect(state.selectedRoom) { if (state.selectedRoom == null) showFullSheet = false }` so `showFullSheet` resets whenever the selection is cleared, preventing it from auto-opening for the next tapped room.
- Replace the existing `state.selectedRoom?.let { RoomBottomSheet(...) }` with:
  ```
  AnimatedVisibility(
      visible = state.selectedRoom != null,
      enter = slideInVertically { it },
      exit = slideOutVertically { it },
      modifier = Modifier.align(Alignment.BottomCenter),
  ) {
      state.selectedRoom?.let { room ->
          RoomPeekCard(
              room = room,
              onDismiss = viewModel::clearSelectedRoom,
              onExpand = { showFullSheet = true },
          )
      }
  }
  if (showFullSheet && state.selectedRoom != null) {
      RoomBottomSheet(
          room = state.selectedRoom!!,
          onDismiss = { showFullSheet = false },
      )
  }
  ```

---

### 3. Pinch-to-Zoom + Pan

**Files changed:** `FloorCanvas.kt` only.

**State (inside `FloorCanvas`):**
```kotlin
var scale by remember { mutableStateOf(1f) }
var offset by remember { mutableStateOf(Offset.Zero) }
val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 4f)
    offset += offsetChange
}
LaunchedEffect(rooms) {
    scale = 1f
    offset = Offset.Zero
}
```

**Canvas modifier chain:**
```kotlin
Canvas(
    modifier = modifier
        .transformable(state = transformState)
        .graphicsLayer {
            scaleX = scale; scaleY = scale
            translationX = offset.x; translationY = offset.y
        }
        .pointerInput(rooms, selectedRoom) { ... }
)
```

**Hit-test coordinate adjustment:**
```kotlin
val normalizedTap = Point(
    x = (tapOffset.x - offset.x) / (size.width * scale),
    y = (tapOffset.y - offset.y) / (size.height * scale),
)
```

**Notes:**
- Scale clamped to `1f..4f`. No lower than 1 (no zooming out past full-fit).
- Offset is not clamped — a future improvement could add boundary clamping, but it's out of scope here.
- `LaunchedEffect(rooms)` resets zoom/pan when the user switches floors.

---

### 4. Map / List Toggle

**Files changed:** `FloorPlanScreen.kt`

**State:** `var showList by remember { mutableStateOf(false) }` (local to Success state block; resets naturally when state changes).

**FAB:** `FloatingActionButton` at `Alignment.BottomEnd` with `padding(16.dp)`.
- Icon: `Icons.Default.FormatListBulleted` when in map mode (tap to see list), `Icons.Default.Map` when in list mode (tap to see map).
- Toggles `showList`.

**List view:** When `showList == true`, replace `FloorCanvas` with a `LazyColumn` of the current floor's `state.rooms`. Each item:
- Room name (`titleSmall`, bold)
- Description (`bodySmall`, `onSurfaceVariant`)
- `Modifier.clickable { viewModel.selectRoom(room); showList = false }`

Tapping a list row: calls `viewModel.selectRoom(room)`, sets `showList = false` (returns to map), and the peek card appears for the selected room.

The floating `FloorSelector` chips remain visible in both map and list modes (they sit in the `Box` overlay layer above both views).

---

## File Change Summary

| File | Change |
|------|--------|
| `FloorPlanScreen.kt` | Box layout, peek card, FAB, list toggle |
| `FloorSelector.kt` | TabRow → FilterChip Row in Surface pill |
| `FloorCanvas.kt` | Transformable state, graphicsLayer, hit-test fix |
| `RoomPeekCard.kt` | New file — compact bottom card component |
| `RoomBottomSheet.kt` | Unchanged |
| `FloorPlanViewModel.kt` | Unchanged |

---

## Testing

Run `./gradlew :composeApp:allTests` before marking done. All floor plan tests must pass unchanged since no domain/VM logic is touched.
