package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme

/**
 * A glow is a very large, very soft colour wash behind the content. Without
 * one the near-black canvas reads as flat ink and the glass surfaces have
 * nothing to sit against.
 *
 * Values are relative to the screen box, so the same numbers work on every
 * device size.
 */
data class DnGlow(
    val color: Color,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val alpha: Float,
)

/**
 * Canvas, ambient light and the top sheen — everything that sits *behind* a
 * screen's content. Light mode needs far stronger glows: a colour at 10 % is
 * clearly visible on near-black and all but invisible on off-white.
 */
@Composable
fun DnBackground(
    modifier: Modifier = Modifier,
    glows: List<DnGlow> = emptyList(),
    content: @Composable BoxScope.() -> Unit,
) {
    val c = DnTheme.colors
    val scale = if (c.isDark) 1f else 2.6f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
    ) {
        if (glows.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                glows.forEach { glow ->
                    val alpha = (glow.alpha * scale).coerceAtMost(0.42f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glow.color.copy(alpha = alpha), Color.Transparent),
                            center = Offset(
                                x = size.width * glow.centerX,
                                y = size.height * glow.centerY,
                            ),
                            radius = size.minDimension * glow.radius,
                        )
                    )
                }
            }
        }
        // top sheen: lifts the black, and in light mode acts as the light source
        Box(
            Modifier
                .fillMaxWidth()
                .height(460.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (c.isDark) 0.05f else 0.72f),
                            Color.Transparent,
                        )
                    )
                )
        )
        if (!c.isDark) {
            // light mode also needs a falloff, otherwise there is no gradient
            // at all and the canvas reads as plain paper
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                c.canvasFalloff,
                            )
                        )
                    )
            )
        }
        content()
    }
}

/**
 * Scroll edge effect: content fades into the canvas before it reaches a
 * floating element, instead of disappearing under a hard edge.
 */
@Composable
fun BoxScope.DnScrollEdge(
    alignment: Alignment = Alignment.BottomCenter,
    height: androidx.compose.ui.unit.Dp = 150.dp,
) {
    val c = DnTheme.colors
    val stops = if (alignment == Alignment.BottomCenter) {
        listOf(Color.Transparent, c.canvas.copy(alpha = 0.75f), c.canvas)
    } else {
        listOf(c.canvas, c.canvas.copy(alpha = 0.92f), Color.Transparent)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .align(alignment)
            .background(Brush.verticalGradient(stops))
    )
}

/** The glow sets used across the app, so screens stay visually related. */
object DnGlows {
    @Composable
    fun action(): List<DnGlow> = listOf(
        DnGlow(DnTheme.colors.lime, 0.75f, -0.1f, 1.1f, 0.10f),
        DnGlow(DnTheme.colors.amber, -0.2f, 0.5f, 0.9f, 0.06f),
    )

    @Composable
    fun information(): List<DnGlow> = listOf(
        DnGlow(DnTheme.colors.blue, 0.7f, -0.1f, 1.1f, 0.10f),
        DnGlow(DnTheme.colors.lime, -0.2f, 0.7f, 0.9f, 0.06f),
    )
}
