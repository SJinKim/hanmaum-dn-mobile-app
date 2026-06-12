# Calendar Event List Screen — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "목록" (list) tab to the existing `CalendarScreen` that shows all events for the current year grouped by month, accessible via a pill segmented control toggle.

**Architecture:** Single `CalendarScreen` with a `ViewMode` enum (`CALENDAR` / `LIST`) stored in `CalendarUiState`. Switching to LIST lazily fetches the full year in one API call via a new `getYearEvents` method. The list renders a `LazyColumn` of 12 month sections (all months always visible), auto-scrolling to the current month on first open.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.0, Ktor 3.3.3, Koin 4.0.0, kotlinx-datetime, Material3

---

### Task 1: Extend CalendarRepository with `getYearEvents`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/domain/repository/CalendarRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/data/repository/CalendarRepositoryImpl.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/calendar/CalendarViewModelTest.kt`

- [ ] **Step 1: Replace CalendarRepository.kt with the new interface**

```kotlin
package com.hanmaum.dn.mobile.features.calendar.domain.repository

import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent

interface CalendarRepository {
    suspend fun getEvents(year: Int, month: Int): Result<List<CalendarEvent>>
    suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>>
}
```

- [ ] **Step 2: Add `getYearEvents` implementation to CalendarRepositoryImpl.kt**

Add this method inside `CalendarRepositoryImpl`, after the `getEvents` override:

```kotlin
override suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>> = runCatching {
    val url = "$GCAL_BASE/$calendarId/events" +
        "?key=$apiKey" +
        "&timeMin=${year}-01-01T00:00:00Z" +
        "&timeMax=${year + 1}-01-01T00:00:00Z" +
        "&orderBy=startTime&singleEvents=true&maxResults=500"
    val body = client.get(url).body<GoogleCalendarEventsResponse>()
    body.items.map { it.toDomain() }
}
```

- [ ] **Step 3: Add `getYearEvents` stub to each anonymous CalendarRepository in CalendarViewModelTest.kt**

There are 4 `object : CalendarRepository { ... }` blocks in the test file. In each one, add after the `getEvents` override:

```kotlin
override suspend fun getYearEvents(year: Int) = Result.success(emptyList<CalendarEvent>())
```

- [ ] **Step 4: Run tests to confirm all existing tests still pass**

```
./gradlew :composeApp:allTests
```

Expected output: `BUILD SUCCESSFUL`, 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/domain/repository/CalendarRepository.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/data/repository/CalendarRepositoryImpl.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/calendar/CalendarViewModelTest.kt
git commit -m "feat(calendar): add getYearEvents to repository"
```

---

### Task 2: Extend CalendarUiState with ViewMode and year-state fields

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarUiState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarViewModel.kt`

- [ ] **Step 1: Replace CalendarUiState.kt entirely**

```kotlin
package com.hanmaum.dn.mobile.features.calendar.presentation

import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent

enum class ViewMode { CALENDAR, LIST }

data class CalendarUiState(
    val year: Int = 2026,
    val month: Int = 1,
    val todayYear: Int = 2026,
    val todayMonth: Int = 1,
    val events: List<CalendarEvent> = emptyList(),
    val selectedDay: Int? = null,
    val selectedEvent: CalendarEvent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val viewMode: ViewMode = ViewMode.CALENDAR,
    val yearEvents: List<CalendarEvent> = emptyList(),
    val yearEventsLoaded: Boolean = false,
    val isYearLoading: Boolean = false,
)
```

- [ ] **Step 2: Update CalendarViewModel `_uiState` initializer to set `todayYear` and `todayMonth`**

In `CalendarViewModel.kt`, replace:

```kotlin
private val _uiState = MutableStateFlow(run {
    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    CalendarUiState(year = now.year, month = now.monthNumber)
})
```

with:

```kotlin
private val _uiState = MutableStateFlow(run {
    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    CalendarUiState(
        year = now.year,
        month = now.monthNumber,
        todayYear = now.year,
        todayMonth = now.monthNumber,
    )
})
```

- [ ] **Step 3: Run tests to confirm nothing broke**

```
./gradlew :composeApp:allTests
```

Expected: `BUILD SUCCESSFUL`, 4 tests pass.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarUiState.kt \
        composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarViewModel.kt
git commit -m "feat(calendar): add ViewMode and year-loading fields to CalendarUiState"
```

---

### Task 3: Add `switchView` + `loadYearEvents` to CalendarViewModel (TDD)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/calendar/CalendarViewModelTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarViewModel.kt`

- [ ] **Step 1: Add three failing tests to CalendarViewModelTest.kt**

Add these imports to the import block:

```kotlin
import com.hanmaum.dn.mobile.features.calendar.presentation.ViewMode
import kotlin.test.assertTrue
```

Add these three tests after the existing 4 tests:

```kotlin
@Test
fun `switchView to LIST loads year events and sets yearEventsLoaded`() = runTest {
    var yearFetchCount = 0
    val repo = object : CalendarRepository {
        override suspend fun getEvents(year: Int, month: Int) = Result.success(emptyList<CalendarEvent>())
        override suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>> {
            yearFetchCount++
            return Result.success(listOf(fakeEvent(1)))
        }
    }
    val vm = CalendarViewModel(repo)
    dispatcher.scheduler.advanceUntilIdle()

    vm.switchView(ViewMode.LIST)
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(ViewMode.LIST, vm.uiState.value.viewMode)
    assertEquals(1, yearFetchCount)
    assertTrue(vm.uiState.value.yearEventsLoaded)
    assertEquals(1, vm.uiState.value.yearEvents.size)
}

@Test
fun `switchView to LIST does not re-fetch when already loaded`() = runTest {
    var yearFetchCount = 0
    val repo = object : CalendarRepository {
        override suspend fun getEvents(year: Int, month: Int) = Result.success(emptyList<CalendarEvent>())
        override suspend fun getYearEvents(year: Int): Result<List<CalendarEvent>> {
            yearFetchCount++
            return Result.success(listOf(fakeEvent(1)))
        }
    }
    val vm = CalendarViewModel(repo)
    dispatcher.scheduler.advanceUntilIdle()

    vm.switchView(ViewMode.LIST)
    dispatcher.scheduler.advanceUntilIdle()
    vm.switchView(ViewMode.CALENDAR)
    vm.switchView(ViewMode.LIST)
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, yearFetchCount)
}

@Test
fun `switchView to LIST sets yearEventsLoaded even when year has no events`() = runTest {
    val repo = object : CalendarRepository {
        override suspend fun getEvents(year: Int, month: Int) = Result.success(emptyList<CalendarEvent>())
        override suspend fun getYearEvents(year: Int) = Result.success(emptyList<CalendarEvent>())
    }
    val vm = CalendarViewModel(repo)
    dispatcher.scheduler.advanceUntilIdle()

    vm.switchView(ViewMode.LIST)
    dispatcher.scheduler.advanceUntilIdle()

    assertTrue(vm.uiState.value.yearEventsLoaded)
    assertEquals(0, vm.uiState.value.yearEvents.size)
}
```

- [ ] **Step 2: Run tests and confirm the 3 new tests fail**

```
./gradlew :composeApp:allTests
```

Expected: BUILD FAILED — 3 tests fail with "unresolved reference: switchView".

- [ ] **Step 3: Add `switchView` and `loadYearEvents` to CalendarViewModel.kt**

Add these two methods to `CalendarViewModel.kt` before the closing brace (after `loadCurrentMonth`):

```kotlin
fun switchView(mode: ViewMode) {
    _uiState.update { it.copy(viewMode = mode) }
    if (mode == ViewMode.LIST && !_uiState.value.yearEventsLoaded) {
        loadYearEvents()
    }
}

private fun loadYearEvents() {
    val year = _uiState.value.year
    _uiState.update { it.copy(isYearLoading = true) }
    viewModelScope.launch {
        repository.getYearEvents(year).fold(
            onSuccess = { events ->
                _uiState.update {
                    it.copy(yearEvents = events, yearEventsLoaded = true, isYearLoading = false)
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(isYearLoading = false, error = err.message ?: "연간 일정 로딩 실패")
                }
            },
        )
    }
}
```

- [ ] **Step 4: Run tests — all 7 must pass**

```
./gradlew :composeApp:allTests
```

Expected: `BUILD SUCCESSFUL`, 7 tests pass.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarViewModel.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/calendar/CalendarViewModelTest.kt
git commit -m "feat(calendar): add switchView and loadYearEvents to CalendarViewModel"
```

---

### Task 4: Add pill toggle and event list UI to CalendarScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarScreen.kt`

Replace the entire file with the content below. Changes from the original:
- `CalendarScreen`: wraps content in a `Column`; adds `ViewModeToggle` + `AnimatedContent` switching between `CalendarContent` and `EventListView`.
- New private composable `ViewModeToggle`: pill segmented control with spring-animated colors.
- Original `LazyColumn` body extracted to `CalendarContent` (no logic changes).
- New private composable `EventListView`: 12-month `LazyColumn` with auto-scroll.
- New private composables `MonthSectionHeader`, `EmptyMonthCard`, `EventListCard`.
- New private function `computeCurrentMonthIndex`.

- [ ] **Step 1: Replace CalendarScreen.kt with the full new content**

```kotlin
package com.hanmaum.dn.mobile.features.calendar.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import org.koin.compose.viewmodel.koinViewModel

private val MONTH_KR = listOf("", "1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월")
private val DAY_HEADERS = listOf("일", "월", "화", "수", "목", "금", "토")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.selectedEvent != null) {
        EventDetailSheet(
            event     = state.selectedEvent!!,
            onDismiss = viewModel::dismissEventDetail,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("캘린더") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ViewModeToggle(
                currentMode  = state.viewMode,
                onModeChange = viewModel::switchView,
            )

            AnimatedContent(
                targetState    = state.viewMode,
                transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
                label          = "calendarViewMode",
            ) { mode ->
                when (mode) {
                    ViewMode.CALENDAR -> CalendarContent(state = state, viewModel = viewModel)
                    ViewMode.LIST     -> EventListView(state = state, onEventClick = viewModel::selectEvent)
                }
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calendarBg by animateColorAsState(
        if (currentMode == ViewMode.CALENDAR) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(), label = "calendarBg",
    )
    val listBg by animateColorAsState(
        if (currentMode == ViewMode.LIST) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(), label = "listBg",
    )
    val calendarTextColor by animateColorAsState(
        if (currentMode == ViewMode.CALENDAR) Color.White else Color(0xFF6B6B6B),
        animationSpec = spring(), label = "calendarText",
    )
    val listTextColor by animateColorAsState(
        if (currentMode == ViewMode.LIST) Color.White else Color(0xFF6B6B6B),
        animationSpec = spring(), label = "listText",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(CircleShape)
            .background(Color(0xFFEBEBEB))
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(calendarBg)
                .clickable { onModeChange(ViewMode.CALENDAR) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "캘린더",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = calendarTextColor,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(listBg)
                .clickable { onModeChange(ViewMode.LIST) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "목록",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = listTextColor,
            )
        }
    }
}

@Composable
private fun CalendarContent(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                verticalAlignment       = Alignment.CenterVertically,
                horizontalArrangement   = Arrangement.SpaceBetween,
                modifier                = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "이전 달")
                }
                Text("${state.year}년 ${MONTH_KR[state.month]}", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "다음 달")
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth()) {
                DAY_HEADERS.forEach { d ->
                    Text(
                        d,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style     = MaterialTheme.typography.labelMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            MonthGrid(
                year        = state.year,
                month       = state.month,
                events      = state.events,
                selectedDay = state.selectedDay,
                onDayClick  = viewModel::selectDay,
            )
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            }
        }

        if (state.error != null) {
            item { Text(state.error!!, color = MaterialTheme.colorScheme.error) }
        }

        val displayEvents = if (state.selectedDay != null) {
            val dayStr = "${state.year}-${state.month.toString().padStart(2, '0')}-${state.selectedDay.toString().padStart(2, '0')}"
            state.events.filter { it.startDate.startsWith(dayStr) }
        } else {
            state.events
        }

        if (displayEvents.isNotEmpty()) {
            item {
                Text(
                    if (state.selectedDay != null) "${state.selectedDay}일 일정" else "이번 달 행사",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(displayEvents, key = { it.id }) { event ->
                EventCard(event = event, onClick = { viewModel.selectEvent(event) })
            }
        } else if (!state.isLoading && state.error == null) {
            item {
                Text(
                    if (state.selectedDay != null) "이 날은 행사가 없습니다" else "이번 달 등록된 행사가 없습니다",
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EventListView(
    state: CalendarUiState,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.yearEventsLoaded) {
        if (state.yearEventsLoaded) {
            val targetIndex = computeCurrentMonthIndex(
                yearEvents   = state.yearEvents,
                year         = state.todayYear,
                currentMonth = state.todayMonth,
            )
            listState.animateScrollToItem(targetIndex)
        }
    }

    if (state.isYearLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (month in 1..12) {
                val monthStr    = month.toString().padStart(2, '0')
                val monthEvents = state.yearEvents.filter { it.startDate.startsWith("${state.todayYear}-$monthStr") }
                val isCurrent   = month == state.todayMonth

                item(key = "header_$month") {
                    MonthSectionHeader(month = month, count = monthEvents.size, isCurrent = isCurrent)
                }

                if (monthEvents.isEmpty()) {
                    item(key = "empty_$month") { EmptyMonthCard() }
                } else {
                    items(monthEvents, key = { "${month}_${it.id}" }) { event ->
                        EventListCard(event = event, onClick = { onEventClick(event) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSectionHeader(month: Int, count: Int, isCurrent: Boolean) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Bottom,
    ) {
        Text(
            MONTH_KR[month],
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.02).sp,
            ),
            color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "${count}개",
            style    = MaterialTheme.typography.labelSmall.copy(
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = (0.05).sp,
            ),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun EmptyMonthCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Text(
            "이벤트 없음",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style    = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color    = Color(0xFFD0D0D0),
        )
    }
}

@Composable
private fun EventListCard(event: CalendarEvent, onClick: () -> Unit) {
    val dayInt  = event.startDate.substring(8, 10).trimStart('0').ifEmpty { "0" }.toInt()
    val dow     = dayOfWeek(
        year  = event.startDate.substring(0, 4).toInt(),
        month = event.startDate.substring(5, 7).toInt(),
        day   = dayInt,
    )
    val timeStr = if (event.isAllDay) "하루 종일"
                  else event.startDate.substringAfterLast('T').take(5)

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.width(32.dp),
            ) {
                Text(
                    "$dayInt",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    DAY_HEADERS[dow],
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (0.05).sp,
                    ),
                    color = Color(0xFFC0C0C0),
                )
            }
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    buildString {
                        append("⏰ $timeStr")
                        if (event.location != null) append("  ·  📍 ${event.location}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    events: List<CalendarEvent>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
) {
    val firstDow    = dayOfWeek(year, month, 1)
    val daysInMonth = daysInMonth(year, month)
    val rows        = (firstDow + daysInMonth + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val day      = row * 7 + col - firstDow + 1
                    val valid    = day in 1..daysInMonth
                    val hasEvent = valid && events.any {
                        it.startDate.startsWith(
                            "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                        )
                    }
                    val isSelected = valid && selectedDay == day

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .then(if (valid) Modifier.clickable { onDayClick(day) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (valid) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                )
                                if (hasEvent) {
                                    Box(
                                        Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: CalendarEvent, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            val timeStr = if (event.isAllDay) "하루 종일"
                          else event.startDate.substringAfterLast('T').take(5)
            Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            if (event.location != null) {
                Text(
                    event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailSheet(event: CalendarEvent, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier            = Modifier.padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val timeStr = if (event.isAllDay) event.startDate
                          else "${event.startDate.substringBefore('T')} ${event.startDate.substringAfterLast('T').take(5)}"
            Text(timeStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            if (event.location != null) {
                Text(
                    "📍 ${event.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!event.description.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerLow)
                Text(
                    event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun computeCurrentMonthIndex(
    yearEvents: List<CalendarEvent>,
    year: Int,
    currentMonth: Int,
): Int {
    var index = 0
    for (m in 1 until currentMonth) {
        val count = yearEvents.count { it.startDate.startsWith("$year-${m.toString().padStart(2, '0')}") }
        index += 1 + maxOf(1, count)
    }
    return index
}

private fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    val m = if (month < 3) month + 12 else month
    val y = if (month < 3) year - 1 else year
    val k = y % 100
    val j = y / 100
    return (((day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j) % 7) + 5) % 7
}

private fun daysInMonth(year: Int, month: Int): Int {
    val d = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    return if (month == 2 && (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0))) 29 else d[month]
}
```

- [ ] **Step 2: Run all tests**

```
./gradlew :composeApp:allTests
```

Expected: `BUILD SUCCESSFUL`, 7 tests pass.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/calendar/presentation/CalendarScreen.kt
git commit -m "feat(calendar): add list tab with year event view to CalendarScreen"
```
