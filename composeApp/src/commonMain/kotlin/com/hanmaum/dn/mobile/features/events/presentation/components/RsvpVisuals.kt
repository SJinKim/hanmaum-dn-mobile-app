package com.hanmaum.dn.mobile.features.events.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.events.domain.model.RsvpStatus

/**
 * The single place the three answers get their colour and glyph.
 *
 * Kept together so a status can never read lime on the sheet and amber on the
 * list — the sheet, the pending cards and the answered rows all resolve here.
 */
data class RsvpVisual(
    val label: String,
    val note: String,
    val icon: ImageVector,
    val accent: Color,
    val dim: Color,
)

@Composable
fun visualFor(status: RsvpStatus): RsvpVisual {
    val c = DnTheme.colors
    return when (status) {
        RsvpStatus.GOING ->
            RsvpVisual("참석", "행사에 참여합니다", DnIcons.Check, c.limeInk, c.limeDim)
        RsvpStatus.NOT_GOING ->
            RsvpVisual("불참", "이번에는 참여가 어렵습니다", DnIcons.X, c.red, c.redDim)
        RsvpStatus.MAYBE ->
            RsvpVisual("미정", "아직 정하지 못했어요", DnIcons.Hourglass, c.amber, c.amberDim)
    }
}

/** One of the three compact answer buttons on a pending card. Shares a row equally. */
@Composable
fun RowScope.RsvpChoiceButton(
    status: RsvpStatus,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = DnTheme.colors
    val v = visualFor(status)
    val shape = RoundedCornerShape(14.dp)

    Row(
        Modifier
            .weight(1f)
            .clip(shape)
            .background(if (selected) v.dim else c.surface2, shape)
            .border(1.dp, if (selected) v.accent.copy(alpha = 0.55f) else c.strokeSubtle, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            v.icon,
            null,
            tint = if (selected) v.accent else c.textSecondary,
            modifier = Modifier.size(15.dp),
        )
        Text(
            v.label,
            style = DnTheme.typography.captionStrong,
            color = if (selected) v.accent else c.textSecondary,
        )
    }
}
