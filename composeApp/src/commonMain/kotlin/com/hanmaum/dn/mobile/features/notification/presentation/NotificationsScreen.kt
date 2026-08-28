package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.hanmaum.dn.mobile.features.notification.domain.model.AppNotification
import org.koin.compose.viewmodel.koinViewModel

/**
 * The notification centre behind the bell on Home.
 *
 * Opening the list marks everything seen, which is what clears the badge —
 * "seen" and "read" are separate on the server, so an unread row keeps its
 * dot until the member actually acts on it.
 */
@Composable
fun NotificationsScreen(onBackClick: () -> Unit) {
    val viewModel: NotificationsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "알림", onBack = onBackClick, onAction = viewModel::markAllRead)

            when (val s = state) {
                is NotificationsUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }

                is NotificationsUiState.Error -> DnErrorState(onRetry = viewModel::load)

                is NotificationsUiState.Success -> if (s.items.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("알림이 없습니다", style = DnTheme.typography.body, color = c.textSecondary)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.items, key = { it.publicId }) { NotificationRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: AppNotification) {
    val c = DnTheme.colors
    // The accent says what the notification is about; unrecognised kinds get
    // the neutral information treatment rather than an arbitrary colour.
    val (icon: ImageVector, ink: Color, container: Color) = when (item.referenceType?.uppercase()) {
        "ANNOUNCEMENT" -> Triple(DnIcons.News, c.blue, c.blueDim)
        "MINISTRY", "TRAINING" -> Triple(DnIcons.UserCheck, c.limeInk, c.limeDim)
        "ATTENDANCE" -> Triple(DnIcons.Check, c.limeInk, c.limeDim)
        "EVENT", "CALENDAR" -> Triple(DnIcons.Calendar, c.blue, c.blueDim)
        else -> Triple(DnIcons.Book, c.amber, c.amberDim)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(c.surface, DnTileShape)
            .border(1.dp, c.strokeSubtle, DnTileShape)
            .clickable { }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = DnTheme.typography.bodyStrong, color = c.textPrimary)
            if (item.body.isNotBlank()) {
                Text(item.body, style = DnTheme.typography.caption, color = c.textSecondary)
            }
            item.createdAt?.let {
                Text(it.take(10), style = DnTheme.typography.label, color = c.textTertiary)
            }
        }

        if (!item.isRead) {
            Spacer(Modifier.size(4.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.limeInk))
        }
    }
}
