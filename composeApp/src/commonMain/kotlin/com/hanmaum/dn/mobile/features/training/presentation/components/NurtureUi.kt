package com.hanmaum.dn.mobile.features.training.presentation.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.hanmaum.dn.mobile.features.training.presentation.detail.CancelOutcome
import com.hanmaum.dn.mobile.features.training.presentation.detail.CancelPrompt

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

/**
 * A centred card with an icon tile and one line: 준비중입니다., or an empty list.
 *
 * [action] adds a button under the message — the 다시 시도 of the 나의 신청 error state. Without
 * it the card stays what it was: a statement about something the member cannot act on.
 */
@Composable
internal fun NurtureMessageCard(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
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
        action?.invoke()
    }
}

/** 503 COURSE_APPLICATION_UNAVAILABLE. No technical detail and no retry: nothing the member can fix. */
@Composable
internal fun NurtureUnavailableCard(modifier: Modifier = Modifier) {
    NurtureMessageCard(DnIcons.Hourglass, LocalStrings.current.nurtureUnavailable, modifier)
}

/**
 * 신청 취소 (#245): asks first, then says what the server answered.
 *
 * The wording follows the status — a running 양육 is stopped, an open application withdrawn —
 * and while the call runs the dialog cannot be dismissed, so a stray tap outside cannot leave
 * the member wondering whether it went through.
 */
@Composable
internal fun NurtureCancelDialog(
    prompt: CancelPrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current

    // Red only for the destructive step and a real failure; amber informs, neutral reports
    // something already settled elsewhere.
    val look = when (prompt.outcome) {
        null -> DialogLook(
            icon = DnIcons.AlertTriangle,
            container = c.redDim,
            ink = c.red,
            title = if (prompt.isAbort) strings.nurtureAbortConfirmTitle else strings.nurtureCancelConfirmTitle,
            body = if (prompt.isAbort) strings.nurtureAbortConfirmBody else strings.nurtureCancelConfirmBody,
        )
        CancelOutcome.NotCancellable -> DialogLook(
            icon = DnIcons.AlertTriangle,
            container = c.amberDim,
            ink = c.amber,
            title = strings.nurtureCancelNotAllowedTitle,
            body = strings.nurtureCancelNotAllowedBody,
        )
        CancelOutcome.NotFound -> DialogLook(
            icon = DnIcons.AlertTriangle,
            container = c.surface2,
            ink = c.textSecondary,
            title = strings.nurtureCancelGoneTitle,
            body = strings.nurtureCancelGoneBody,
        )
        CancelOutcome.Unavailable -> DialogLook(
            icon = DnIcons.Hourglass,
            container = c.surface2,
            ink = c.textSecondary,
            title = strings.nurtureUnavailable,
            body = strings.nurtureUnavailableContact,
        )
        CancelOutcome.Failed -> DialogLook(
            icon = DnIcons.AlertTriangle,
            container = c.redDim,
            ink = c.red,
            title = strings.nurtureCancelFailedTitle,
            body = strings.nurtureCancelFailedBody,
        )
    }

    Dialog(onDismissRequest = { if (prompt.isDismissable) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconTile(look.icon, container = look.container, ink = look.ink)
            Spacer(Modifier.height(14.dp))
            Text(look.title, style = DnTheme.typography.headline, color = c.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                look.body,
                style = DnTheme.typography.caption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            CancelActions(prompt = prompt, onConfirm = onConfirm, onDismiss = onDismiss)
        }
    }
}

/**
 * Before the call: keep or go through with it. After it: 확인 only, unless the failure was one
 * a retry can fix.
 */
@Composable
private fun CancelActions(prompt: CancelPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val outcome = prompt.outcome

    if (outcome != null && !outcome.isRetryable) {
        DnPrimaryButton(label = strings.confirm, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        return
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DialogButton(
            label = strings.nurtureCancelKeep,
            container = c.surface2,
            ink = c.textPrimary,
            enabled = prompt.isDismissable,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
        )
        if (outcome?.isRetryable == true) {
            DialogButton(
                label = strings.retry,
                container = c.lime,
                ink = c.onLime,
                enabled = prompt.canConfirm,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        } else {
            DialogButton(
                label = when {
                    prompt.isCancelling -> strings.nurtureCancelBusy
                    prompt.isAbort -> strings.nurtureAbortConfirmAction
                    else -> strings.nurtureCancelConfirmAction
                },
                container = c.red,
                ink = c.onRed,
                enabled = prompt.canConfirm,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    container: Color,
    ink: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(DnPillShape)
            .background(container, DnPillShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = DnTheme.typography.captionStrong, color = ink, textAlign = TextAlign.Center)
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
private fun IconTile(
    icon: ImageVector,
    container: Color = DnTheme.colors.surface3,
    ink: Color = DnTheme.colors.textSecondary,
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(DnTileShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(22.dp))
    }
}

/** How one state of the cancel dialog looks; the colour carries the meaning. */
private data class DialogLook(
    val icon: ImageVector,
    val container: Color,
    val ink: Color,
    val title: String,
    val body: String,
)

private const val DISABLED_ALPHA = 0.4f
