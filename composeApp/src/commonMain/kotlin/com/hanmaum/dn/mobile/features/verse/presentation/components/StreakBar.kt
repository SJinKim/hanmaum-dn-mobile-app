package com.hanmaum.dn.mobile.features.verse.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseStreak
import kotlinx.datetime.LocalDate

private val PillShape = RoundedCornerShape(3.dp)

/**
 * Seven pills and a ratio: the week's marks for one verse card.
 *
 * The pills stay at the design's 24×6 dp. The touch target does not: the whole
 * row is 44 dp tall and clickable, with today's pill as the thing the member
 * aims at. Widening or heightening one pill to reach 44 dp would break the
 * bar's rhythm, and a 6 dp-tall tap target is not one.
 *
 * Marking is one-way. The row stops being clickable the moment today is marked,
 * and there is no gesture that removes a mark — the server exposes no delete.
 */
@Composable
fun StreakBar(
    streak: VerseStreak,
    today: LocalDate,
    onMarkToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val canMark = streak.todayMarkable && !streak.todayMarked
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = canMark,
                onClick = onMarkToday,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            streak.week.forEach { day ->
                val marked = streak.isMarked(day)
                val isToday = day == today
                val markable = streak.isMarkable(day)

                // A day that can never be filled reads dimmest, a marked one
                // brightest, and today sits between the two so the state that
                // invites a tap is not the least visible one.
                val fill by animateColorAsState(
                    targetValue = when {
                        marked -> c.amber
                        isToday && canMark -> c.amberDim
                        !markable -> c.surface2
                        else -> c.surface3
                    },
                    animationSpec = spring(),
                    label = "pillFill",
                )

                Box(
                    Modifier
                        .size(width = 24.dp, height = 6.dp)
                        .clip(PillShape)
                        .background(fill, PillShape)
                        .then(
                            // amberDim alone renders darker than an empty pill in
                            // dark mode, which inverts the hierarchy. The stroke
                            // restores it and is legible at 6 dp.
                            if (isToday && canMark) Modifier.border(1.dp, c.amber, PillShape)
                            else Modifier,
                        ),
                )
            }
        }

        Text(
            strings.verseStreakRatio(streak.markedThisWeek, streak.markableDays.size),
            style = DnTheme.typography.caption,
            color = c.textTertiary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
