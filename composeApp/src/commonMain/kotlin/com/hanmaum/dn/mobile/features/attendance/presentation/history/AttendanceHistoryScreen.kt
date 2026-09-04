package com.hanmaum.dn.mobile.features.attendance.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.toLocalDateTime
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnMonthGrid
import com.hanmaum.dn.mobile.core.presentation.components.DnMonthNav
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.components.DnWeekdayHeader
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceEntry
import org.koin.compose.viewmodel.koinViewModel

/**
 * 출석 확인 — the member's own attendance as a calendar.
 *
 * Deliberately the same grid, month navigation and list structure as the events
 * calendar, from the shared components in `core/presentation/components`. Only
 * the meaning of a mark differs: there it is an event, here it is a 예배 the
 * member actually attended.
 */
@Composable
fun AttendanceHistoryScreen(onBackClick: () -> Unit) {
    val viewModel: AttendanceHistoryViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.attendanceHistoryTitle, onBack = onBackClick, actionIcon = null)

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
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
                item { DnWeekdayHeader() }
                item {
                    DnMonthGrid(
                        year = state.year,
                        month = state.month,
                        selectedDay = state.selectedDay,
                        // Marks come only from confirmed own attendance.
                        markedDays = state.markedDays,
                        onDayClick = viewModel::selectDay,
                    )
                }

                when {
                    state.isLoading -> item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                            CircularProgressIndicator(color = c.lime)
                        }
                    }

                    state.failed -> item {
                        // A calendar without its data is useless, so this offers
                        // a way back — unlike the tiles on 출석 체크, where a
                        // dash is a tolerable outcome.
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(c.surface, RoundedCornerShape(24.dp))
                                .border(1.dp, c.strokeSubtle, RoundedCornerShape(24.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(DnIcons.AlertTriangle, null, tint = c.red, modifier = Modifier.size(20.dp))
                            Text(
                                strings.attendanceLoadFailed,
                                style = DnTheme.typography.captionStrong,
                                color = c.textSecondary,
                            )
                            DnPrimaryButton(strings.attendanceRetry, viewModel::load)
                        }
                    }

                    else -> {
                        val entries = state.entriesForSelectedDay
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    strings.dayTitle(state.month, state.selectedDay),
                                    style = DnTheme.typography.headline,
                                    color = c.textPrimary,
                                )
                                Text(
                                    strings.attendanceCount.replace("{n}", entries.size.toString()),
                                    style = DnTheme.typography.caption,
                                    color = c.textTertiary,
                                )
                            }
                        }

                        if (entries.isEmpty()) {
                            item {
                                EmptyCard(
                                    // Nothing on this day is a different fact
                                    // from nothing at all, and reads differently.
                                    if (state.attended.isEmpty()) strings.attendanceNoRecords
                                    else strings.attendanceNoneThisDay,
                                )
                            }
                        } else {
                            items(entries, key = { "${it.date}-${it.definitionPublicId}-${it.checkedInAt}" }) {
                                AttendanceRow(it, strings)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface, RoundedCornerShape(24.dp))
            .border(1.dp, c.strokeSubtle, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = DnTheme.typography.captionStrong, color = c.textSecondary)
    }
}

@Composable
private fun AttendanceRow(entry: AttendanceEntry, strings: AppStrings) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface, RoundedCornerShape(24.dp))
            .border(1.dp, c.strokeSubtle, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.lime),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entry.definitionTitle, style = DnTheme.typography.captionStrong, color = c.textPrimary)
            Text(
                strings.attendanceCheckedInAt.replace("{time}", timeOf(entry.checkedInAt)),
                style = DnTheme.typography.caption,
                color = c.textTertiary,
            )
        }
    }
}

/**
 * "09:12" out of an ISO instant, without pulling in a formatter.
 *
 * The server sends UTC. Showing it verbatim would be wrong by the offset, so
 * the instant is converted to the device's zone first — the same zone the rest
 * of the app uses to decide what "today" means.
 */
private fun timeOf(iso: String?): String {
    if (iso == null) return "—"
    return try {
        val local = kotlin.time.Instant.parse(iso)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    } catch (_: IllegalArgumentException) {
        "—"
    }
}

/** "8월 16일 예배", or the month alone when no day is selected. */
private fun AppStrings.dayTitle(month: Int, day: Int?): String {
    val monthName = months.getOrNull(month).orEmpty()
    if (day == null) return monthName
    return attendanceDayServices.replace("{month}", monthName).replace("{day}", day.toString())
}
