package com.hanmaum.dn.mobile.features.training.presentation.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnPillShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus

/*
 * Building blocks of the 양육 list and detail. Figma: section `22 · 양육 신청 · Zustände`.
 */

/** A small status tag: 신청 가능 / 신청 마감 on the list, the application status on the detail. */
@Composable
internal fun NurtureBadge(label: String, container: Color, ink: Color) {
    Box(
        Modifier
            .clip(DnPillShape)
            .background(container, DnPillShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = DnTheme.typography.label, color = ink)
    }
}

@Composable
internal fun NurtureOpenBadge(open: Boolean) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    if (open) {
        NurtureBadge(strings.nurtureOpen, c.limeDim, c.limeInk)
    } else {
        NurtureBadge(strings.nurtureClosed, c.redDim, c.red)
    }
}

/**
 * Blue = received, lime = confirmed or running, neutral = finished or unknown, red = dropped.
 */
@Composable
internal fun NurtureStatusBadge(status: TrainingApplicationStatus) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    when (status) {
        TrainingApplicationStatus.APPLIED -> NurtureBadge(strings.nurtureStatusApplied, c.blueDim, c.blue)
        TrainingApplicationStatus.ENROLLED -> NurtureBadge(strings.nurtureStatusEnrolled, c.limeDim, c.limeInk)
        TrainingApplicationStatus.IN_PROGRESS -> NurtureBadge(strings.nurtureStatusInProgress, c.limeDim, c.limeInk)
        TrainingApplicationStatus.COMPLETED -> NurtureBadge(strings.nurtureStatusCompleted, c.surface3, c.textSecondary)
        TrainingApplicationStatus.DROPPED -> NurtureBadge(strings.nurtureStatusDropped, c.redDim, c.red)
        TrainingApplicationStatus.UNKNOWN -> NurtureBadge(strings.nurtureStatusUnknown, c.surface3, c.textSecondary)
    }
}

/** A centred card with an icon tile and one line: 준비중입니다., or an empty list. */
@Composable
internal fun NurtureMessageCard(icon: ImageVector, message: String, modifier: Modifier = Modifier) {
    val c = DnTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.surface, DnCardShape)
            .border(1.dp, c.strokeSubtle, DnCardShape)
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon)
        Text(
            message,
            style = DnTheme.typography.bodyStrong,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** 503 COURSE_APPLICATION_UNAVAILABLE. No technical detail and no retry: nothing the member can fix. */
@Composable
internal fun NurtureUnavailableCard(modifier: Modifier = Modifier) {
    NurtureMessageCard(DnIcons.Hourglass, LocalStrings.current.nurtureUnavailable, modifier)
}

/**
 * Cancelling has no server endpoint yet, so 신청 취소 explains that instead of calling one.
 * The application stays as it is.
 */
@Composable
internal fun NurtureCancelDialog(onDismiss: () -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconTile(DnIcons.Hourglass, container = c.surface2)
            Spacer(Modifier.height(14.dp))
            Text(
                strings.nurtureUnavailable,
                style = DnTheme.typography.headline,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                strings.nurtureUnavailableContact,
                style = DnTheme.typography.caption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            DnPrimaryButton(
                label = strings.confirm,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A grey bar standing in for text while the page loads, so the layout does not jump. */
@Composable
internal fun NurtureSkeleton(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(width)
            .height(height)
            .clip(DnInnerShape)
            .background(DnTheme.colors.surface3),
    )
}

/** Two placeholder rows at list-row height. */
@Composable
internal fun NurtureListSkeleton(modifier: Modifier = Modifier) {
    val c = DnTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(DnCardShape)
                    .background(c.surface, DnCardShape)
                    .border(1.dp, c.strokeSubtle, DnCardShape)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(DnTileShape)
                        .background(c.surface3),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NurtureSkeleton(110.dp, 14.dp)
                    NurtureSkeleton(190.dp, 12.dp)
                    NurtureSkeleton(90.dp, 10.dp)
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, container: Color = DnTheme.colors.surface3) {
    Box(
        Modifier
            .size(48.dp)
            .clip(DnTileShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = DnTheme.colors.textSecondary, modifier = Modifier.size(22.dp))
    }
}
