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
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import org.koin.compose.viewmodel.koinViewModel

private val DAY_HEADERS = listOf("일", "월", "화", "수", "목", "금", "토")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = koinViewModel()) {
    val strings = LocalStrings.current
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
                title = { Text(strings.navCalendar) },
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
    val strings = LocalStrings.current
    val calendarBg by animateColorAsState(
        if (currentMode == ViewMode.CALENDAR) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(), label = "calendarBg",
    )
    val listBg by animateColorAsState(
        if (currentMode == ViewMode.LIST) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(), label = "listBg",
    )
    val calendarTextColor by animateColorAsState(
        if (currentMode == ViewMode.CALENDAR) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(), label = "calendarText",
    )
    val listTextColor by animateColorAsState(
        if (currentMode == ViewMode.LIST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(), label = "listText",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                strings.navCalendar,
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
                strings.list,
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
    val strings = LocalStrings.current
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
                Text("${state.year}${strings.yearSuffix} ${strings.months[state.month]}", style = MaterialTheme.typography.titleLarge)
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
            item { Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
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
    val strings = LocalStrings.current
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Bottom,
    ) {
        Text(
            strings.months[month],
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
            color    = MaterialTheme.colorScheme.outline,
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
                    color = MaterialTheme.colorScheme.outline,
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
