# Floor Plan Filter Bar Redesign — Design Spec

**Date:** 2026-05-08
**Feature:** `features/floorplan`
**Scope:** UI-only changes. No ViewModel, repository, or backend changes required.

---

## Problem

The floating `FloorSelector` pill overlaps the map canvas at `TopStart`. The `FloatingActionButton` for list toggle also overlaps at `BottomEnd`. Users lose map real estate to UI controls that float on top of the content rather than sitting beside it.

---

## Goals

1. Separate the filter controls from the map — no overlay, no occlusion.
2. Combine floor selector + room location chips into a single two-row filter bar.
3. Room chip tap highlights the selected room on canvas and slides up the peek card.
4. "목록" button in the filter bar toggles list mode; "✕ 목록" in TopAppBar closes it.
5. TopAppBar title and back arrow are explicitly visible (no white-on-white).

---

## Out of Scope

- ViewModel, repository, or backend changes
- Animations beyond existing peek card slide
- Changes to `FloorCanvas.kt`, `RoomPeekCard.kt`, or `RoomBottomSheet.kt`

---

## Design

### Layout Structure

Replace the current `Box`-with-overlays approach with a `Column` inside the Scaffold content area:

```
Scaffold
  topBar: TopAppBar (+ "✕ 목록" action in list mode)
  content:
    Column(fillMaxSize)
      ├── FloorPlanFilterBar   ← shown only in map mode
      └── Box(weight = 1f)
            ├── FloorCanvas (map mode)  OR  LazyColumn (list mode)
            └── AnimatedVisibility → RoomPeekCard (map mode only)
    + RoomBottomSheet (if showFullSheet)
```

---

### 1. State Hoisting

`showList` must be hoisted **above** the Scaffold so the TopAppBar can reference it:

```kotlin
var showList by remember { mutableStateOf(false) }
LaunchedEffect(uiState) {
    if (uiState !is FloorPlanUiState.Success) showList = false
}
```

`showFullSheet` stays inside the `Success` block (it only matters when there is a selected room).

`LaunchedEffect(state.selectedFloor) { showList = false }` — reset list mode on floor change (already present, keep it).

---

### 2. TopAppBar Changes

**File:** `FloorPlanScreen.kt`

Explicitly set content colors to prevent white-on-white on any theme:

```kotlin
colors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
)
```

Add an `actions` slot that shows "✕ 목록" only in list mode:

```kotlin
actions = {
    if (showList) {
        TextButton(onClick = { showList = false }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("목록", style = MaterialTheme.typography.labelMedium)
        }
    }
}
```

---

### 3. FloorPlanFilterBar Component

**New file:** `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorPlanFilterBar.kt`

**Signature:**
```kotlin
@Composable
fun FloorPlanFilterBar(
    floors: List<Floor>,
    selectedFloor: Floor,
    rooms: List<Room>,
    selectedRoom: Room?,
    onFloorSelected: (Floor) -> Unit,
    onRoomSelected: (Room) -> Unit,
    onRoomDeselected: () -> Unit,
    onShowList: () -> Unit,
    modifier: Modifier = Modifier,
)
```

**Layout:**

```kotlin
Surface(
    modifier = modifier.fillMaxWidth(),
    tonalElevation = 2.dp,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        // Row 1: floor chips + 목록 button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            floors.forEach { floor ->
                FilterChip(
                    selected = floor.id == selectedFloor.id,
                    onClick = { onFloorSelected(floor) },
                    label = { Text(floor.name) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onShowList,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("목록", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Row 2: scrollable room chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            rooms.forEach { room ->
                val selected = room.id == selectedRoom?.id
                FilterChip(
                    selected = selected,
                    onClick = { if (selected) onRoomDeselected() else onRoomSelected(room) },
                    label = { Text(room.name) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
    }
}
```

**Interaction rules:**
- Tapping an unselected room chip → `onRoomSelected(room)` → `viewModel.selectRoom(room)` → highlights room on canvas + slides up peek card.
- Tapping the already-selected room chip → `onRoomDeselected()` → `viewModel.clearSelectedRoom()` → clears highlight and dismisses peek card.
- Floor chip tap → `onFloorSelected(floor)` → `viewModel.selectFloor(floor)` → room chips update to new floor's rooms.

---

### 4. FloorPlanScreen.kt Restructure

**File:** `FloorPlanScreen.kt`

Full Success-state content layout:

```kotlin
is FloorPlanUiState.Success -> {
    var showFullSheet by remember { mutableStateOf(false) }
    LaunchedEffect(state.selectedRoom) {
        if (state.selectedRoom == null) showFullSheet = false
    }
    LaunchedEffect(state.selectedFloor) {
        showList = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!showList) {
            FloorPlanFilterBar(
                floors = state.floors,
                selectedFloor = state.selectedFloor,
                rooms = state.rooms,
                selectedRoom = state.selectedRoom,
                onFloorSelected = viewModel::selectFloor,
                onRoomSelected = viewModel::selectRoom,
                onRoomDeselected = viewModel::clearSelectedRoom,
                onShowList = { showList = true },
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (showList) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(state.rooms) { room ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = room.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            supportingContent = if (room.description.isNotBlank()) {
                                { Text(room.description, style = MaterialTheme.typography.bodySmall) }
                            } else null,
                            modifier = Modifier.clickable {
                                viewModel.selectRoom(room)
                                showList = false
                            },
                        )
                        HorizontalDivider()
                    }
                }
            } else {
                FloorCanvas(
                    rooms = state.rooms,
                    selectedRoom = state.selectedRoom,
                    onRoomTap = viewModel::selectRoom,
                    onEmptyTap = viewModel::clearSelectedRoom,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedVisibility(
                visible = state.selectedRoom != null && !showList,
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
        }
    }

    if (showFullSheet && state.selectedRoom != null) {
        RoomBottomSheet(
            room = state.selectedRoom!!,
            onDismiss = { showFullSheet = false },
        )
    }
}
```

---

### 5. FloorSelector.kt

`FloorSelector.kt` is no longer referenced by `FloorPlanScreen.kt` after this change. **Delete it.**

---

## File Change Summary

| File | Change |
|------|--------|
| `FloorPlanScreen.kt` | Hoist `showList`, Column layout, TopAppBar color fix + "✕ 목록" action, remove FAB + FloorSelector overlay |
| `FloorPlanFilterBar.kt` | New file — two-row filter bar component |
| `FloorSelector.kt` | Deleted |
| `FloorCanvas.kt` | Unchanged |
| `RoomPeekCard.kt` | Unchanged |
| `RoomBottomSheet.kt` | Unchanged |
| `FloorPlanViewModel.kt` | Unchanged |

---

## Testing

Run `./gradlew :composeApp:allTests` before marking done. No domain/VM logic is changed so all existing floor plan tests must pass.

**Manual checks:**
- TopAppBar title and back arrow are dark/visible on both light and dark theme.
- Floor chip tap updates room chips row and resets to map mode.
- Room chip tap highlights room on canvas and shows peek card.
- Tapping selected room chip deselects (clears highlight and peek card).
- "☰ 목록" tap enters list mode (filter bar hidden, "✕ 목록" appears in TopAppBar).
- "✕ 목록" tap returns to map mode (filter bar reappears, button disappears).
- Tapping a list row selects room, returns to map, shows peek card.
- Peek card "↑" button opens full bottom sheet.
