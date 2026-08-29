package com.hanmaum.dn.mobile.core.presentation.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme

/**
 * True backdrop blur — blurring whatever sits *behind* a surface — is not
 * portable in Compose Multiplatform. Android needs `RenderEffect` (API 31+),
 * iOS needs interop with `UIVisualEffectView`, and neither is available
 * through common code today.
 *
 * Until that lands, [dnGlass] paints the opaque stand-in colours from the
 * palette. They are the translucent glass values pre-composited over the
 * canvas, so spacing, contrast and visual weight stay identical — only the
 * refraction is missing. Devices that cannot blur keep this look permanently.
 *
 * When a blur implementation is added, flip [backdropBlurSupported] and the
 * translucent branch takes over everywhere at once.
 */
expect fun backdropBlurSupported(): Boolean

enum class GlassLevel {
    /** chips, circular icon buttons, peek cards */
    Regular,

    /** the floating dock and floating action bars */
    Strong,
}

/**
 * Applies the navigation-layer glass material.
 *
 * Per the iOS 26 rules this belongs on bars, floating controls and overlay
 * buttons only — never on content cards, list rows or sheets.
 */
fun Modifier.dnGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    level: GlassLevel = GlassLevel.Regular,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
): Modifier = composed {
    val colors = DnTheme.colors
    val blurred = backdropBlurSupported()

    val fill = when {
        blurred && level == GlassLevel.Strong -> colors.glassFillStrong
        blurred                               -> colors.glassFill
        level == GlassLevel.Strong            -> colors.glassFillStrongOpaque
        else                                  -> colors.glassFillOpaque
    }
    val border = if (blurred) colors.glassStroke else colors.strokeSubtle

    this
        .background(color = fill, shape = shape)
        .border(width = borderWidth, color = border, shape = shape)
}

/**
 * The opaque surface used for everything on the content layer: cards, list
 * rows, tiles, inputs and sheets. Kept next to [dnGlass] so the distinction
 * stays visible at the call site.
 */
@Composable
fun Modifier.dnSurface(
    shape: Shape = RoundedCornerShape(28.dp),
    elevated: Boolean = false,
): Modifier {
    val colors = DnTheme.colors
    return this
        .background(color = if (elevated) colors.surface else colors.surface2, shape = shape)
        .border(width = 1.dp, color = colors.strokeSubtle, shape = shape)
}
