package com.hanmaum.dn.mobile.features.notification.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.components.dnPressable
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType

@Composable
fun NotificationRow(
    notification: Notification,
    timeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DnTheme.colors

    // The accent says what the notification is about. An unrecognised type gets
    // the neutral bell rather than borrowing another kind's colour — a wrong
    // signal reads worse than no signal.
    val icon: ImageVector
    val ink: Color
    val container: Color
    when (notification.type) {
        NotificationType.ANNOUNCEMENT -> { icon = DnIcons.News; ink = c.blue; container = c.blueDim }
        NotificationType.EVENT_REMINDER -> { icon = DnIcons.Calendar; ink = c.amber; container = c.amberDim }
        NotificationType.UNKNOWN -> { icon = DnIcons.Bell; ink = c.textSecondary; container = c.surface2 }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .dnPressable(shape = DnTileShape, onClick = onClick)
            .background(c.surface, DnTileShape)
            .border(1.dp, c.strokeSubtle, DnTileShape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                notification.title,
                // Unread carries its weight in the title, not only in the dot —
                // the dot is easy to miss on a long list.
                style = if (notification.isRead) DnTheme.typography.captionStrong else DnTheme.typography.bodyStrong,
                color = if (notification.isRead) c.textSecondary else c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (notification.body.isNotBlank()) {
                Text(
                    notification.body,
                    style = DnTheme.typography.caption,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(timeText, style = DnTheme.typography.label, color = c.textTertiary)
        }

        if (!notification.isRead) {
            Spacer(Modifier.size(4.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.limeInk))
        }
    }
}
