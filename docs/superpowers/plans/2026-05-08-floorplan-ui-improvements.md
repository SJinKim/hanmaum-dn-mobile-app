# Floor Plan UI Improvements — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the floor plan screen with four UI improvements: full-screen canvas, peek card, pinch-to-zoom/pan, and map/list toggle.

**Architecture:** All changes are UI-only — no ViewModel, repository, or backend modifications. New `RoomPeekCard.kt` component added; `FloorSelector.kt`, `FloorCanvas.kt`, and `FloorPlanScreen.kt` modified.

**Tech Stack:** Compose Multiplatform 1.10.0, Material3, `material-icons-extended` (already in `build.gradle.kts`), Compose Foundation gestures (`rememberTransformableState`, `transformable`)

---

### Task 1: Create Feature Branch

**Files:** (none)

- [ ] **Step 1: Create and checkout branch**

```bash
git checkout -b feat/floorplan-ui-improvements
```

Expected: `Switched to a new branch 'feat/floorplan-ui-improvements'`

---

### Task 2: Redesign FloorSelector — Floating Pill

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorSelector.kt`

Replace the full-width `TabRow` with a `Surface(CircleShape)` wrapping a `Row` of `FilterChip`s. This makes the selector compact enough to float over the canvas.

- [ ] **Step 1: Overwrite FloorSelector.kt**

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorSelector(
    floors: List<Floor>,
    selectedFloor: Floor,
    onFloorSelected: (Floor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            floors.forEach { floor ->
                FilterChip(
                    selected = floor.id == selectedFloor.id,
                    onClick = { onFloorSelected(floor) },
                    label = { Text(floor.name, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorSelector.kt
git commit -m "refactor(floorplan): replace TabRow with floating FilterChip pill selector"
```

---

### Task 3: Box Layout in FloorPlanScreen — Full-Screen Canvas + Floating Selector

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt`

Replace `Column { FloorSelector + FloorCanvas }` with a `Box`. Canvas gets `fillMaxSize()`. Selector floats at `TopStart`. Remove the `verticalScroll` and `aspectRatio` workarounds — they were compensating for the half-screen problem.

**Note:** This step is an intermediate state — `RoomBottomSheet` is still wired directly. The peek card replaces it in Task 5.

- [ ] **Step 1: Overwrite FloorPlanScreen.kt**

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
                    FloorCanvas(
                        rooms = state.rooms,
                        selectedRoom = state.selectedRoom,
                        onRoomTap = viewModel::selectRoom,
                        onEmptyTap = viewModel::clearSelectedRoom,
                        modifier = Modifier.fillMaxSize(),
                    )
                    FloorSelector(
                        floors = state.floors,
                        selectedFloor = state.selectedFloor,
                        onFloorSelected = viewModel::selectFloor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 12.dp),
                    )
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

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt
git commit -m "feat(floorplan): full-screen canvas with floating floor selector overlay"
```

---

### Task 4: Create RoomPeekCard Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/RoomPeekCard.kt`

Compact bottom card showing room name + description excerpt. Tapping the up-arrow opens the full `RoomBottomSheet`.

- [ ] **Step 1: Create RoomPeekCard.kt**

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room

@Composable
fun RoomPeekCard(
    room: Room,
    onDismiss: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (room.description.isNotBlank()) {
                    Text(
                        text = room.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "자세히 보기",
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/RoomPeekCard.kt
git commit -m "feat(floorplan): add RoomPeekCard compact bottom overlay component"
```

---

### Task 5: Wire Peek Card + Full Sheet in FloorPlanScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt`

Replace the direct `RoomBottomSheet` with `AnimatedVisibility` + `RoomPeekCard`. The full sheet only opens when the user taps the expand button. `LaunchedEffect(state.selectedRoom)` resets `showFullSheet` whenever selection is cleared so the sheet doesn't auto-reopen for the next tapped room.

- [ ] **Step 1: Overwrite FloorPlanScreen.kt with peek card wiring**

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorSelector
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
                    var showFullSheet by remember { mutableStateOf(false) }
                    LaunchedEffect(state.selectedRoom) {
                        if (state.selectedRoom == null) showFullSheet = false
                    }

                    FloorCanvas(
                        rooms = state.rooms,
                        selectedRoom = state.selectedRoom,
                        onRoomTap = viewModel::selectRoom,
                        onEmptyTap = viewModel::clearSelectedRoom,
                        modifier = Modifier.fillMaxSize(),
                    )
                    FloorSelector(
                        floors = state.floors,
                        selectedFloor = state.selectedFloor,
                        onFloorSelected = viewModel::selectFloor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 12.dp),
                    )
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
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt
git commit -m "feat(floorplan): replace full bottom sheet with animated peek card + expand flow"
```

---

### Task 6: Add Pinch-to-Zoom + Pan to FloorCanvas

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorCanvas.kt`

Add `rememberTransformableState` for pinch-zoom (clamped 1×–4×) and pan. Apply the transform via `graphicsLayer` with `TransformOrigin(0f, 0f)` (top-left pivot). Adjust the tap hit-test formula to reverse the transform: `(tapOffset.x - offset.x) / (size.width * scale)`.

**Why top-left origin:** With the pivot at (0,0), the visual position of a canvas point at normalized `(px, py)` is `(px * width * scale + offset.x, py * height * scale + offset.y)`. The inverse (tap → normalized) is simply `(tapOffset.x - offset.x) / (width * scale)` — no pivot correction needed.

**Why `LaunchedEffect(rooms)`:** `rooms` changes when the user switches floors. Resetting scale and offset on floor change keeps each floor view at 1× zoom.

- [ ] **Step 1: Overwrite FloorCanvas.kt**

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
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
    val surfaceContainerLowest = MaterialTheme.colorScheme.surfaceContainerLowest
    val onSurface = MaterialTheme.colorScheme.onSurface

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

    Canvas(
        modifier = modifier
            .transformable(state = transformState)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .pointerInput(rooms, selectedRoom) {
                detectTapGestures { tapOffset ->
                    val normalizedTap = Point(
                        x = (tapOffset.x - offset.x) / (size.width * scale),
                        y = (tapOffset.y - offset.y) / (size.height * scale),
                    )
                    val hit = rooms.firstOrNull { it.points.hitTest(normalizedTap) }
                    if (hit != null) onRoomTap(hit) else onEmptyTap()
                }
            },
    ) {
        drawRect(surfaceContainerLow)
        rooms.forEach { room ->
            val scaled = room.points.map { Offset(it.x * size.width, it.y * size.height) }
            if (scaled.isEmpty()) return@forEach

            val path = Path().apply {
                moveTo(scaled.first().x, scaled.first().y)
                scaled.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }

            val fillColor = if (room.id == selectedRoom?.id) primaryContainer else surfaceContainerLowest
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

**Compiler note:** `.graphicsLayer { }` is a Modifier extension. If the IDE reports an unresolved reference, use Alt+Enter to auto-import — it will suggest `import androidx.compose.ui.graphics.graphicsLayer`.

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/components/FloorCanvas.kt
git commit -m "feat(floorplan): add pinch-to-zoom and pan with hit-test coordinate adjustment"
```

---

### Task 7: Add Map / List Toggle FAB

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt`

Add a `FloatingActionButton` at `BottomEnd` that toggles between map view and a `LazyColumn` room list. The FAB is positioned at `bottom = 104.dp` so it always clears the ~88dp peek card area. Tapping a list item selects the room and returns to map view. The floating `FloorSelector` stays visible in both modes.

- [ ] **Step 1: Overwrite FloorPlanScreen.kt with the final complete version**

```kotlin
package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorSelector
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
                    var showList by remember { mutableStateOf(false) }
                    var showFullSheet by remember { mutableStateOf(false) }
                    LaunchedEffect(state.selectedRoom) {
                        if (state.selectedRoom == null) showFullSheet = false
                    }

                    if (showList) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
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

                    FloorSelector(
                        floors = state.floors,
                        selectedFloor = state.selectedFloor,
                        onFloorSelected = viewModel::selectFloor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 12.dp),
                    )

                    FloatingActionButton(
                        onClick = { showList = !showList },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 104.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Icon(
                            imageVector = if (showList) Icons.Default.Map else Icons.Default.FormatListBulleted,
                            contentDescription = if (showList) "지도 보기" else "목록 보기",
                        )
                    }

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
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/floorplan/presentation/FloorPlanScreen.kt
git commit -m "feat(floorplan): add map/list toggle FAB with room discovery list view"
```

---

### Task 8: Run All Tests + Final Verification

**Files:** (none)

- [ ] **Step 1: Run the full test suite**

```bash
./gradlew :composeApp:allTests
```

Expected: `BUILD SUCCESSFUL` — all existing floor plan tests pass. No ViewModel or domain logic was modified, so no test changes are required.

- [ ] **Step 2: Verify no failures in output**

Look for lines like:
```
> Task :composeApp:testDebugUnitTest
...
BUILD SUCCESSFUL
```

If any floor plan tests fail, they indicate a regression in the hit-test logic or UI state management — review `FloorCanvas.kt` coordinate math and `FloorPlanScreen.kt` state handling.

---

## File Change Summary

| File | Change |
|------|--------|
| `FloorSelector.kt` | TabRow → FilterChip Row inside Surface(CircleShape) pill |
| `FloorPlanScreen.kt` | Box layout, floating selector, AnimatedVisibility peek card, FAB, list toggle |
| `FloorCanvas.kt` | `rememberTransformableState`, `graphicsLayer`, hit-test coordinate inversion |
| `RoomPeekCard.kt` | **New file** — compact bottom card with expand button |
| `RoomBottomSheet.kt` | Unchanged |
| `FloorPlanViewModel.kt` | Unchanged |
