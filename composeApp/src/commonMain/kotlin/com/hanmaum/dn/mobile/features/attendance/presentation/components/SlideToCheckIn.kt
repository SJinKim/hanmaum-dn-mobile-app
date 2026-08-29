package com.hanmaum.dn.mobile.features.attendance.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import kotlin.math.roundToInt

/**
 * Slide to check in.
 *
 * A deliberate gesture rather than a button: checking in is a one-shot action
 * that cannot be undone from the app, so it should be hard to trigger by
 * accident. The track fills as you drag and the whole control turns solid
 * green once it succeeds.
 */
@Composable
fun SlideToCheckIn(
    label: String,
    enabled: Boolean,
    checkedIn: Boolean,
    isBusy: Boolean,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
    disabledLabel: String = label,
) {
    val c = DnTheme.colors
    val density = LocalDensity.current
    val knob = 52.dp
    val inset = 6.dp

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var dragPx by remember { mutableFloatStateOf(0f) }

    val knobPx = with(density) { knob.toPx() }
    val insetPx = with(density) { inset.toPx() }
    val maxDrag = (trackWidthPx - knobPx - insetPx * 2).coerceAtLeast(1f)

    val settled = when {
        checkedIn -> maxDrag
        else -> dragPx.coerceIn(0f, maxDrag)
    }
    val animatedOffset by animateFloatAsState(
        targetValue = settled,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkInKnob",
    )
    val progress = (animatedOffset / maxDrag).coerceIn(0f, 1f)

    val container = when {
        checkedIn -> c.lime
        !enabled -> c.surface2
        else -> c.limeDim
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(32.dp))
            .background(container, RoundedCornerShape(32.dp))
            .border(
                width = 1.dp,
                color = if (checkedIn) c.lime else if (enabled) c.lime.copy(alpha = 0.28f) else c.strokeSubtle,
                shape = RoundedCornerShape(32.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // the trail behind the knob, so the drag reads as progress
        if (!checkedIn && progress > 0.01f) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(64.dp)
                    .align(Alignment.CenterStart)
                    .background(c.lime.copy(alpha = 0.35f))
            )
        }

        Text(
            text = when {
                checkedIn -> label
                enabled -> label
                else -> disabledLabel
            },
            style = DnTheme.typography.bodyStrong,
            color = when {
                checkedIn -> c.onLime
                enabled -> c.limeInk
                else -> c.textTertiary
            },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 24.dp, end = 24.dp),
        )

        if (enabled && !checkedIn) {
            Icon(
                imageVector = DnIcons.ChevronsRight,
                contentDescription = null,
                tint = c.limeInk.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 22.dp)
                    .size(22.dp),
            )
        }

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = inset)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .size(knob)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (checkedIn) c.onLime else if (enabled) c.lime else c.surface3)
                .then(
                    if (enabled && !checkedIn && !isBusy) {
                        Modifier.draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                dragPx = (dragPx + delta).coerceIn(0f, maxDrag)
                            },
                            onDragStopped = {
                                if (dragPx > maxDrag * 0.75f) onCheckIn() else dragPx = 0f
                            },
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isBusy -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = c.onLime,
                )
                checkedIn -> Icon(DnIcons.Check, null, tint = c.lime, modifier = Modifier.size(24.dp))
                enabled -> Icon(DnIcons.UserCheck, null, tint = c.onLime, modifier = Modifier.size(24.dp))
                else -> Icon(DnIcons.Clock, null, tint = c.textTertiary, modifier = Modifier.size(24.dp))
            }
        }
    }
}
