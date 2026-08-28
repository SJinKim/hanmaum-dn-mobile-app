package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/** Shared building blocks for the 사역 and 양육 detail pages. */

@Composable
internal fun DetailHero(
    icon: ImageVector,
    container: Color,
    ink: Color,
    eyebrow: String,
    name: String,
) {
    val c = DnTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(eyebrow, style = DnTheme.typography.label, color = ink)
            Text(name, style = DnTheme.typography.titleLg, color = c.textPrimary)
        }
    }
}

@Composable
internal fun DetailFacts(rows: List<Triple<ImageVector, String, String>>) {
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.blueDim, DnCardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { (icon, label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.blue),
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
    }
}

@Composable
internal fun DetailSection(title: String, body: String) {
    val c = DnTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = DnTheme.typography.headline, color = c.textPrimary)
        Text(body, style = DnTheme.typography.body, color = c.textSecondary)
    }
}

/** Capacity read-out — the number that decides whether applying is worth it. */
@Composable
internal fun DetailCapacity(taken: Int?, total: Int?, deadline: String?) {
    val c = DnTheme.colors
    val progress = if (taken != null && total != null && total > 0) {
        taken.toFloat() / total.toFloat()
    } else 0f

    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.surface, DnCardShape)
            .border(1.dp, c.strokeSubtle, DnCardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("신청 현황", style = DnTheme.typography.captionStrong, color = c.textPrimary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    taken?.toString() ?: "–",
                    style = DnTheme.typography.captionStrong,
                    color = c.limeInk,
                )
                Text(
                    " / ${total?.toString() ?: "–"}명",
                    style = DnTheme.typography.caption,
                    color = c.textTertiary,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.surface3),
        ) {
            if (progress > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.lime)
                )
            }
        }
        Text(
            deadline?.let { "마감 $it" } ?: "마감일 미정",
            style = DnTheme.typography.label,
            color = c.textTertiary,
        )
    }
}

/** Floating action bar carrying the single primary action of a detail page. */
@Composable
internal fun DetailActionBar(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) { content() }
}

@Composable
internal fun Spacer12() = Spacer(Modifier.height(12.dp))
