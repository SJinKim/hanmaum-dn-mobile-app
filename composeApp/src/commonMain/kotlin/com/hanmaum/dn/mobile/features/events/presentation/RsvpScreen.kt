package com.hanmaum.dn.mobile.features.events.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus
import com.hanmaum.dn.mobile.features.events.presentation.components.RsvpChoiceButton
import com.hanmaum.dn.mobile.features.events.presentation.components.visualFor
import org.koin.compose.viewmodel.koinViewModel

/**
 * 행사 참석 — every open invitation in one place.
 *
 * This is where "나중에" leads. Answers stay changeable while the window is
 * open, so the screen never becomes a dead record of past taps.
 */
@Composable
fun RsvpScreen(
    onBackClick: () -> Unit,
    viewModel: EventRsvpViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    // This screen holds its own ViewModel instance — the ones in EventRsvpHost
    // and the 출석 체크 banner are scoped elsewhere and never touch it. Without
    // this the state stays at its initial isLoading = true and the spinner
    // never stops.
    LaunchedEffect(Unit) { viewModel.refresh() }

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "행사 참석", onBack = onBackClick, actionIcon = null)

            when {
                state.isLoading && state.events.isEmpty() ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = c.lime)
                    }

                state.error != null && state.events.isEmpty() ->
                    DnErrorState(onRetry = viewModel::refresh)

                state.events.isEmpty() -> EmptyRsvp()

                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(20.dp))
                    SummaryRow(state)

                    if (state.pending.isNotEmpty()) {
                        Spacer(Modifier.height(22.dp))
                        SectionHeader("응답 필요", "마감 임박순")
                        Spacer(Modifier.height(12.dp))
                        state.pending.forEach { event ->
                            PendingCard(
                                event = event,
                                busy = state.respondingTo == event.publicId,
                                enabled = state.respondingTo == null,
                                error = state.rowErrors[event.publicId],
                                onRespond = { status -> viewModel.respond(event.publicId, status) },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    if (state.answered.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        SectionHeader("응답 완료", "변경 가능")
                        Spacer(Modifier.height(12.dp))
                        state.answered.forEach { event ->
                            AnsweredRow(
                                event = event,
                                enabled = state.respondingTo == null,
                                onRespond = { status -> viewModel.respond(event.publicId, status) },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(state: EventRsvpUiState) {
    val c = DnTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("응답 필요", state.pendingCount, c.amber),
            Triple("참석", state.goingCount, c.limeInk),
            Triple("불참", state.notGoingCount, c.red),
        ).forEach { (label, value, accent) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(DnTileShape)
                    .background(c.surface, DnTileShape)
                    .border(1.dp, c.strokeSubtle, DnTileShape)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("$value", style = DnTheme.typography.stat, color = accent)
                Text(label, style = DnTheme.typography.label, color = c.textTertiary)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, hint: String) {
    val c = DnTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = DnTheme.typography.headline, color = c.textPrimary)
        Text(hint, style = DnTheme.typography.caption, color = c.textTertiary)
    }
}

@Composable
private fun PendingCard(
    event: EventRsvp,
    busy: Boolean,
    enabled: Boolean,
    error: String?,
    onRespond: (RsvpStatus) -> Unit,
) {
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(c.surface, DnTileShape)
            .border(1.dp, c.strokeSubtle, DnTileShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.title, style = DnTheme.typography.bodyStrong, color = c.textPrimary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(DnIcons.Clock, null, tint = c.textTertiary, modifier = Modifier.size(14.dp))
                    Text(
                        RsvpFormat.deadline(event.windowEnd),
                        style = DnTheme.typography.caption,
                        color = c.textTertiary,
                    )
                }
            }
            DeadlinePill(event)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RsvpStatus.entries.forEach { status ->
                RsvpChoiceButton(
                    status = status,
                    selected = event.myStatus == status,
                    enabled = enabled && !busy,
                    onClick = { onRespond(status) },
                )
            }
        }

        // Only shown when the server says a reminder is actually pending —
        // the app does not know the configured offsets and must not guess.
        event.nextReminderAt?.takeIf { event.myStatus == RsvpStatus.MAYBE }?.let { at ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DnIcons.Bell, null, tint = c.amber, modifier = Modifier.size(13.dp))
                Text(RsvpFormat.reminderHint(at), style = DnTheme.typography.caption, color = c.amber)
            }
        }

        error?.let { Text(it, style = DnTheme.typography.caption, color = c.red) }
    }
}

/**
 * "D-3" — how much room is left to answer.
 *
 * Amber only once it is close; a deadline nine days out is information, not a
 * warning, and colouring it would spend urgency the screen may need later.
 */
@Composable
private fun DeadlinePill(event: EventRsvp) {
    val c = DnTheme.colors
    val text = RsvpFormat.countdown(event.windowEnd)
    val urgent = text == "D-DAY" || (text.removePrefix("D-").toIntOrNull() ?: 99) <= 3
    val ink: Color = if (urgent) c.amber else c.textTertiary
    val fill: Color = if (urgent) c.amberDim else c.surface2

    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(fill, RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = DnTheme.typography.label, color = ink)
    }
}

@Composable
private fun AnsweredRow(
    event: EventRsvp,
    enabled: Boolean,
    onRespond: (RsvpStatus) -> Unit,
) {
    val c = DnTheme.colors
    val status = event.myStatus ?: return
    val v = visualFor(status)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(c.surface, DnTileShape)
            .border(1.dp, c.strokeSubtle, DnTileShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(v.dim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(v.icon, null, tint = v.accent, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(event.title, style = DnTheme.typography.captionStrong, color = c.textPrimary)
                Text(
                    event.respondedAt?.let { RsvpFormat.respondedOn(it) }
                        ?: RsvpFormat.deadline(event.windowEnd),
                    style = DnTheme.typography.caption,
                    color = c.textTertiary,
                )
            }
            Text(v.label, style = DnTheme.typography.label, color = v.accent)
        }

        // The answer stays changeable while the window is open — a mind changed
        // two days before the event is still worth more than a stale headcount.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RsvpStatus.entries.forEach { s ->
                RsvpChoiceButton(
                    status = s,
                    selected = status == s,
                    enabled = enabled,
                    onClick = { onRespond(s) },
                )
            }
        }
    }
}

@Composable
private fun EmptyRsvp() {
    val c = DnTheme.colors
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(c.limeDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.Calendar, null, tint = c.limeInk, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("예정된 행사가 없습니다", style = DnTheme.typography.captionStrong, color = c.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "참석 확인이 필요한 행사가 생기면 여기에 표시됩니다",
                style = DnTheme.typography.caption,
                color = c.textTertiary,
            )
        }
    }
}
