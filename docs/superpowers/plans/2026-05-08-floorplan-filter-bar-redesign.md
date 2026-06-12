# Floor Plan Filter Bar Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace floating FloorSelector/FAB overlays with a two-row filter bar (floor chips + room chips) that sits above the map, and add a "✕ 목록" toggle in the TopAppBar for list mode.

**Architecture:** `showList` state is hoisted above the Scaffold so both TopAppBar actions and the content body can reference it. A new `FloorPlanFilterBar` composable owns both chip rows; `FloorPlanScreen` uses a `Column` to stack the filter bar above a `Box` holding the canvas or list. `FloorSelector.kt` is deleted as it is fully replaced.

**Tech Stack:** Compose Multiplatform 1.10.0, Material3, `material-icons-extended`

---

## File Map

| Action | Path |
|--------|------|
| **Create** | `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorPlanFilterBar.kt` |
| **Modify** | `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt` |
| **Delete** | `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorSelector.kt` |

No ViewModel, domain, data, or test file changes — this is UI-only.

---

### Task 1: Create FloorPlanFilterBar.kt

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorPlanFilterBar.kt`

There are no unit tests to write for this composable (no test infrastructure for Compose UI in `commonTest`). Correctness is verified by the build and manual check.

- [ ] **Step 1: Create the file with full implementation**

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room

@OptIn(ExperimentalMaterial3Api::class)
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
) {
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
                        label = { Text(floor.name, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
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
                    Spacer(modifier = Modifier.width(4.dp))
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
}
```

- [ ] **Step 2: Verify the file compiles**

```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` — no unresolved references.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorPlanFilterBar.kt
git commit -m "feat(floorplan): add FloorPlanFilterBar with floor chips and room location chips"
```

---

### Task 2: Restructure FloorPlanScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt`

- [ ] **Step 1: Replace the entire file contents**

Replace `FloorPlanScreen.kt` with the following. Key changes from the current file:
- `showList` hoisted above `Scaffold` so TopAppBar actions can read it
- TopAppBar gains explicit `titleContentColor`, `navigationIconContentColor`, and an `actions` block showing "✕ 목록" when `showList == true`
- Success state uses `Column` instead of `Box`; filter bar is first child (hidden in list mode); content area is `Box(Modifier.weight(1f))`
- `FloatingActionButton` and floating `FloorSelector` are removed
- `LazyColumn` content padding simplified to `PaddingValues(bottom = 16.dp)` (no top padding needed — filter bar is a separate row now)

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorCanvas
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorPlanFilterBar
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.RoomBottomSheet
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.RoomPeekCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: FloorPlanViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showList by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (uiState !is FloorPlanUiState.Success) showList = false
    }

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
                        )
                    }
                },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
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
                                                {
                                                    Text(
                                                        room.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
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
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` — no reference to `FloorSelector` remains (it is no longer imported).

- [ ] **Step 3: Run all tests**

```bash
./gradlew :composeApp:allTests 2>&1 | tail -30
```

Expected: All tests pass. The data-layer `FloorPlanRepositoryImplTest` is the only floor plan test and must remain green.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt
git commit -m "feat(floorplan): restructure screen to Column layout with two-row filter bar"
```

---

### Task 3: Delete FloorSelector.kt

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorSelector.kt`

- [ ] **Step 1: Confirm no remaining references to FloorSelector**

```bash
grep -r "FloorSelector" composeApp/src/commonMain/ --include="*.kt"
```

Expected: no output. If any file still imports `FloorSelector`, fix it before deleting.

- [ ] **Step 2: Delete the file**

```bash
rm composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorSelector.kt
```

- [ ] **Step 3: Verify the build is still clean**

```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run all tests one final time**

```bash
./gradlew :composeApp:allTests 2>&1 | tail -30
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add -u composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorSelector.kt
git commit -m "refactor(floorplan): delete FloorSelector — replaced by FloorPlanFilterBar"
```
