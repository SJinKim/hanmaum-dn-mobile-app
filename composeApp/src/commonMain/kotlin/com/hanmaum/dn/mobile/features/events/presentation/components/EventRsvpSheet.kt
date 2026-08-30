package com.hanmaum.dn.mobile.features.events.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus
import com.hanmaum.dn.mobile.features.events.presentation.RsvpFormat

/**
 * The first ask: one event, three answers, and a way out.
 *
 * Only ever offered for a genuinely unanswered event — see the ViewModel. It
 * shows a single event rather than a list because a stack of prompts is a wall,
 * not a question; anything further waits on the RSVP screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventRsvpSheet(
    event: EventRsvp,
    isResponding: Boolean,
    errorMessage: String?,
    onRespond: (RsvpStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = DnTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        scrimColor = c.inverse.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), Alignment.Center) {
                Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(c.strokeSubtle))
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 34.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(DnIcons.Calendar, null, tint = c.limeInk, modifier = Modifier.size(15.dp))
                Text("행사 참석 확인", style = DnTheme.typography.label, color = c.limeInk)
            }

            Spacer(Modifier.height(8.dp))
            Text(event.title, style = DnTheme.typography.title, color = c.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "${RsvpFormat.date(event.windowEnd)}까지 응답해 주세요 · 인원 확정에 필요합니다",
                style = DnTheme.typography.caption,
                color = c.textSecondary,
            )

            Spacer(Modifier.height(16.dp))
            RsvpStatus.entries.forEach { status ->
                OptionRow(
                    status = status,
                    selected = event.myStatus == status,
                    enabled = !isResponding,
                    // MAYBE is the one answer that earns an extra line: it is the
                    // only one the server follows up on, and saying so up front
                    // stops the reminder feeling like it came out of nowhere.
                    extraNote = if (status == RsvpStatus.MAYBE) "마감 1주 전 다시 알림" else null,
                    onClick = { onRespond(status) },
                )
                Spacer(Modifier.height(8.dp))
            }

            errorMessage?.let {
                Text(it, style = DnTheme.typography.caption, color = c.red)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(DnTileShape)
                    .border(1.dp, c.strokeSubtle, DnTileShape)
                    .clickable(enabled = !isResponding, onClick = onDismiss)
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("나중에 답하기", style = DnTheme.typography.captionStrong, color = c.textTertiary)
            }
        }
    }
}

@Composable
private fun OptionRow(
    status: RsvpStatus,
    selected: Boolean,
    enabled: Boolean,
    extraNote: String?,
    onClick: () -> Unit,
) {
    val c = DnTheme.colors
    val v = visualFor(status)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(if (selected) v.dim else c.surface2, DnTileShape)
            .border(1.dp, if (selected) v.accent else c.strokeSubtle, DnTileShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                v.label,
                style = DnTheme.typography.bodyStrong,
                color = if (selected) v.accent else c.textPrimary,
            )
            Text(
                listOfNotNull(v.note, extraNote).joinToString(" · "),
                style = DnTheme.typography.caption,
                color = c.textTertiary,
            )
        }

        when {
            !enabled && selected ->
                CircularProgressIndicator(color = v.accent, modifier = Modifier.size(22.dp))
            selected -> Box(
                Modifier.size(26.dp).clip(CircleShape).background(v.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.Check, null, tint = c.onLime, modifier = Modifier.size(15.dp))
            }
            else -> Box(
                Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, c.strokeStrong, CircleShape),
            )
        }
    }
}
