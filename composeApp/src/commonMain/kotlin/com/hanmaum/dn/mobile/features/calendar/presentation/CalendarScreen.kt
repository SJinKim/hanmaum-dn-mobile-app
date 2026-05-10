package com.hanmaum.dn.mobile.features.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Month navigation header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
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

            // Day-of-week headers
            item {
                Row(Modifier.fillMaxWidth()) {
                    DAY_HEADERS.forEach { d ->
                        Text(
                            d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Month grid
            item {
                MonthGrid(
                    year        = state.year,
                    month       = state.month,
                    events      = state.events,
                    selectedDay = state.selectedDay,
                    onDayClick  = viewModel::selectDay,
                )
            }

            // Loading indicator
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                }
            }

            // Error
            if (state.error != null) {
                item { Text(state.error!!, color = MaterialTheme.colorScheme.error) }
            }

            // Events list — filtered to selected day if set, else all this month
            val displayEvents = if (state.selectedDay != null) {
                val dayStr = "${state.year}-${state.month.toString().padStart(2, '0')}-${state.selectedDay.toString().padStart(2, '0')}"
                state.events.filter { it.startDate.startsWith(dayStr) }
            } else {
                state.events
            }

            if (displayEvents.isNotEmpty()) {
                item {
                    Text(
                        if (state.selectedDay != null) "${state.selectedDay}일 일정"
                        else "이번 달 행사",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(displayEvents, key = { it.id }) { event ->
                    EventCard(event = event, onClick = { viewModel.selectEvent(event) })
                }
            } else if (!state.isLoading && state.error == null) {
                item {
                    Text(
                        if (state.selectedDay != null) "이 날은 행사가 없습니다"
                        else "이번 달 등록된 행사가 없습니다",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
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
                    val day   = row * 7 + col - firstDow + 1
                    val valid = day in 1..daysInMonth
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
            modifier  = Modifier.padding(16.dp),
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
            modifier = Modifier.padding(24.dp).navigationBarsPadding(),
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

// Zeller's congruence — returns 0=Sun … 6=Sat
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
