package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/**
 * The month calendar, shared by the events calendar and the attendance history.
 *
 * These were private to `CalendarScreen` until #164 needed the same grid for a
 * second purpose. They take plain values rather than a UI state, so the two
 * callers can mark days for entirely different reasons — events in one case,
 * the member's own attendance in the other — without either knowing about the
 * other's model.
 */

/** Zero-padded to two digits, for building ISO date keys. */
fun pad2(n: Int): String = n.toString().padStart(2, '0')

/**
 * Sunday-based weekday index (0 = Sunday … 6 = Saturday) via Zeller's congruence.
 *
 * Zeller yields 0 = Saturday, so the shift to a Sunday-based week is +6. The
 * version inherited from CalendarScreen used +5 and placed every month one
 * column to the left — 1 August 2026 is a Saturday and landed under 금. Nothing
 * failed, because the grid was internally consistent; it was simply wrong.
 */
fun dayOfWeekIndex(year: Int, month: Int, day: Int): Int {
    val m = if (month < 3) month + 12 else month
    val y = if (month < 3) year - 1 else year
    val k = y % 100
    val j = y / 100
    return (((day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j) % 7) + 6) % 7
}

fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
}

/** Previous / month label / next. */
@Composable
fun DnMonthNav(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DnCircleIconButton(rotate = true, onClick = onPrevious)
        Text(
            "$year${strings.yearSuffix} ${strings.months[month]}",
            style = DnTheme.typography.title,
            color = c.textPrimary,
        )
        DnCircleIconButton(rotate = false, onClick = onNext)
    }
}

@Composable
fun DnCircleIconButton(rotate: Boolean, onClick: () -> Unit) {
    val c = DnTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(c.surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            DnIcons.ChevronRight,
            null,
            tint = c.textSecondary,
            modifier = Modifier.size(18.dp).rotate(if (rotate) 180f else 0f),
        )
    }
}

/** 일 월 화 수 목 금 토 */
@Composable
fun DnWeekdayHeader(modifier: Modifier = Modifier) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Row(modifier.fillMaxWidth()) {
        strings.dayHeaders.forEach { d ->
            Text(
                d,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = DnTheme.typography.label,
                color = c.textTertiary,
            )
        }
    }
}

/**
 * The day grid. [markedDays] are days of this month that carry a dot; what a
 * mark means is the caller's business.
 *
 * The dot keeps its space even when it is not shown, so a marked day and an
 * unmarked one line their numbers up.
 */
@Composable
fun DnMonthGrid(
    year: Int,
    month: Int,
    selectedDay: Int?,
    markedDays: Set<Int>,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DnTheme.colors
    val firstDow = dayOfWeekIndex(year, month, 1)
    val days = daysInMonth(year, month)
    val rows = (firstDow + days + 6) / 7

    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val day = row * 7 + col - firstDow + 1
                    val valid = day in 1..days
                    val selected = valid && selectedDay == day
                    val marked = valid && day in markedDays
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (selected) c.lime else Color.Transparent)
                            .then(if (valid) Modifier.clickable { onDayClick(day) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (valid) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    "$day",
                                    style = DnTheme.typography.captionStrong,
                                    color = if (selected) c.onLime else c.textPrimary,
                                )
                                Box(
                                    Modifier
                                        .size(4.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(
                                            when {
                                                !marked -> Color.Transparent
                                                selected -> c.onLime
                                                else -> c.limeInk
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
