package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.hanmaum.dn.mobile.core.presentation.theme.AppMotion

/** Target scale for a press: [AppMotion.PRESS_SCALE] while held, 1 otherwise and whenever disabled. */
internal fun pressScale(pressed: Boolean, enabled: Boolean): Float =
    if (pressed && enabled) AppMotion.PRESS_SCALE else 1f

/**
 * The one place the press feedback from DESIGN.md is implemented: a click target that
 * scales to [AppMotion.PRESS_SCALE] with [AppMotion.press] and springs back.
 *
 * Put it first in the chain, before `background` / `border` / `padding`, so the scale
 * carries the whole surface. [shape] clips the ripple to the surface; pass the same shape
 * the background uses.
 */
fun Modifier.dnPressable(
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(pressScale(pressed, enabled), AppMotion.press, label = "dnPress")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
}
