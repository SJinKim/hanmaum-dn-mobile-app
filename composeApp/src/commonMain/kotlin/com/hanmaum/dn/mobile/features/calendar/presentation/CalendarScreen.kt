package com.hanmaum.dn.mobile.features.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnDock
import com.hanmaum.dn.mobile.core.presentation.components.DnScrollEdge
import com.hanmaum.dn.mobile.core.presentation.components.DnSegmented
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.components.DnMonthGrid
import com.hanmaum.dn.mobile.core.presentation.components.DnMonthNav
import com.hanmaum.dn.mobile.core.presentation.components.DnWeekdayHeader
import com.hanmaum.dn.mobile.core.presentation.components.pad2
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.calendar.domain.model.CalendarEvent
import org.koin.compose.viewmodel.koinViewModel

/**
 * 캘린더 — month grid with a day agenda, or a flat list for the year.
 *
 * Tapping an event opens a sheet rather than a pushed screen: the view model
 * already models the selection as an overlay (`selectedEvent` /
 * `dismissEventDetail`), and turning it into a route would have meant
 * changing navigation, which is out of scope for a UI pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBackClick: () -> Unit) {
    val viewModel: CalendarViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.navCalendar, onBack = onBackClick)

            Spacer(Modifier.height(12.dp))
            DnSegmented(
                options = listOf("월간", "목록"),
                selectedIndex = if (state.viewMode == ViewMode.CALENDAR) 0 else 1,
                onSelect = { viewModel.switchView(if (it == 0) ViewMode.CALENDAR else ViewMode.LIST) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))

            if (state.viewMode == ViewMode.CALENDAR) {
                MonthView(state = state, viewModel = viewModel)
            } else {
                YearListView(state = state, onEventClick = viewModel::selectEvent)
            }
        }

        DnScrollEdge()
    }

    state.selectedEvent?.let { event ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissEventDetail,
            containerColor = c.surface,
        ) {
            EventSheet(event)
        }
    }
}

@Composable
private fun MonthView(state: CalendarUiState, viewModel: CalendarViewModel) {
    val c = DnTheme.colors
    val strings = LocalStrings.current

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = DnDock.contentInset()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            DnMonthNav(
                year = state.year,
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
            )
        }

        item {
            DnWeekdayHeader()
        }

        item {
            DnMonthGrid(
                year = state.year,
                month = state.month,
                selectedDay = state.selectedDay,
                markedDays = state.events.mapNotNullTo(mutableSetOf()) { event ->
                    val prefix = "${state.year}-${pad2(state.month)}-"
                    event.startDate.takeIf { it.startsWith(prefix) }
                        ?.substring(prefix.length, prefix.length + 2)?.toIntOrNull()
                },
                onDayClick = viewModel::selectDay,
            )
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }
            }
        }

        val dayEvents = state.selectedDay?.let { day ->
            val key = "${state.year}-${pad2(state.month)}-${pad2(day)}"
            state.events.filter { it.startDate.startsWith(key) }
        } ?: state.events

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                state.selectedDay?.let { "${it}일 일정" } ?: strings.calendarEventsThisMonth,
                style = DnTheme.typography.headline,
                color = c.textPrimary,
            )
        }

        if (dayEvents.isEmpty()) {
            item {
                Text(
                    if (state.selectedDay != null) strings.calendarNoEventsThisDay
                    else strings.calendarNoEvents,
                    style = DnTheme.typography.caption,
                    color = c.textTertiary,
                )
            }
        } else {
            items(dayEvents, key = { it.id }) { event ->
                EventRow(event) { viewModel.selectEvent(event) }
            }
        }
    }
}

@Composable
private fun YearListView(state: CalendarUiState, onEventClick: (CalendarEvent) -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current

    if (state.isYearLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = c.lime) }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = DnDock.contentInset()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (month in 1..12) {
            val monthEvents = state.yearEvents.filter {
                it.startDate.startsWith("${state.todayYear}-${pad2(month)}")
            }
            item(key = "h$month") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        strings.months[month],
                        style = DnTheme.typography.titleLg,
                        color = if (month == state.todayMonth) c.limeInk else c.textPrimary,
                    )
                    Text(
                        "${monthEvents.size}개",
                        style = DnTheme.typography.label,
                        color = c.textTertiary,
                    )
                }
            }
            if (monthEvents.isEmpty()) {
                item(key = "e$month") {
                    Text(
                        strings.calendarNoEvents,
                        style = DnTheme.typography.caption,
                        color = c.textTertiary,
                    )
                }
            } else {
                items(monthEvents, key = { "${month}_${it.id}" }) { event ->
                    EventRow(event) { onEventClick(event) }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, onClick: () -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(c.surface, DnTileShape)
            .border(1.dp, c.strokeSubtle, DnTileShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.lime)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(event.title, style = DnTheme.typography.captionStrong, color = c.textPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(DnIcons.Clock, null, tint = c.textTertiary, modifier = Modifier.size(13.dp))
                Text(
                    if (event.isAllDay) strings.calendarAllDay
                    else event.startDate.substringAfterLast('T').take(5),
                    style = DnTheme.typography.caption,
                    color = c.textTertiary,
                )
                event.location?.let {
                    Icon(DnIcons.MapPin, null, tint = c.textTertiary, modifier = Modifier.size(13.dp))
                    Text(it, style = DnTheme.typography.caption, color = c.textTertiary)
                }
            }
        }
        Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EventSheet(event: CalendarEvent) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(event.title, style = DnTheme.typography.titleLg, color = c.textPrimary)
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnTileShape)
                .background(c.blueDim, DnTileShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetFact(
                DnIcons.Calendar, "일시",
                if (event.isAllDay) "${event.startDate.take(10)} · ${strings.calendarAllDay}"
                else "${event.startDate.take(10)} ${event.startDate.substringAfterLast('T').take(5)}",
            )
            event.location?.let { SheetFact(DnIcons.MapPin, "장소", it) }
        }

        if (!event.description.isNullOrBlank()) {
            Spacer(Modifier.height(18.dp))
            Text(event.description, style = DnTheme.typography.body, color = c.textSecondary)
        }

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun SheetFact(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val c = DnTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(c.blue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = c.onBlue, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = DnTheme.typography.label, color = c.textTertiary)
            Text(value, style = DnTheme.typography.captionStrong, color = c.textPrimary)
        }
    }
}

