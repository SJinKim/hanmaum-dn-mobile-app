package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design system v2 — "dark first".
 *
 * Material3's ColorScheme has no slot for the glass material or for the
 * accent containers, so the full token set lives here and is published through
 * [LocalDnColors]. Every value is taken straight from the Figma collection
 * "DN Theme"; the two modes are tuned independently rather than inverted.
 *
 * Contrast ratios in the comments are measured against the mode's own canvas
 * (WCAG 2.2). Normal text needs 4.5:1, large text and UI shapes 3:1.
 */
@Immutable
data class DnColors(
    // ── ground and surfaces ───────────────────────────────────────────
    val canvas: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val inverse: Color,

    // ── edges ─────────────────────────────────────────────────────────
    val strokeSubtle: Color,
    val strokeStrong: Color,

    // ── text ──────────────────────────────────────────────────────────
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInverse: Color,

    // ── accent · action and attendance ────────────────────────────────
    val lime: Color,
    val onLime: Color,
    val limeDim: Color,
    /** lime used as text or icon; darker in light mode so it clears 4.5:1 */
    val limeInk: Color,

    // ── accent · information and dates ────────────────────────────────
    val blue: Color,
    val onBlue: Color,
    val blueDim: Color,

    // ── accent · word and devotion ────────────────────────────────────
    val amber: Color,
    val onAmber: Color,
    val amberDim: Color,

    // ── accent · notification and error ───────────────────────────────
    val red: Color,
    val onRed: Color,
    val redDim: Color,

    // ── glass (navigation layer only) ─────────────────────────────────
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassStroke: Color,
    val glassHairline: Color,

    /**
     * Opaque stand-ins used wherever the platform cannot blur what sits
     * behind a surface. They are the translucent values already composited
     * over the canvas, so the layout keeps the same visual weight.
     */
    val glassFillOpaque: Color,
    val glassFillStrongOpaque: Color,

    /** tinted falloff at the foot of the canvas — carries the light-mode depth */
    val canvasFalloff: Color,

    /** media viewers sit on true black in both themes so nothing competes with the image */
    val mediaBackdrop: Color,

    val isDark: Boolean,
)

val DnDarkColors = DnColors(
    canvas       = Color(0xFF0B0D10),
    surface      = Color(0xFF14171C),
    surface2     = Color(0xFF1B1F26),
    surface3     = Color(0xFF262B34),
    inverse      = Color(0xFFF2F5F8),

    strokeSubtle = Color(0xFF262B34),
    strokeStrong = Color(0xFF39404B),

    textPrimary   = Color(0xFFF2F5F8), // 17.8 : 1
    textSecondary = Color(0xFF9AA5B1), //  7.8 : 1
    textTertiary  = Color(0xFF7C8794), //  5.3 : 1
    textInverse   = Color(0xFF0B0D10),

    lime    = Color(0xFFB4EC55),
    onLime  = Color(0xFF0D1206), // 13.6 : 1 on lime
    limeDim = Color(0xFF2C3A15),
    limeInk = Color(0xFFB4EC55), // 14.0 : 1

    blue    = Color(0xFF6FB0FF), //  8.6 : 1
    onBlue  = Color(0xFF05121F), //  8.4 : 1 on blue
    blueDim = Color(0xFF14263C),

    amber    = Color(0xFFF5BB4D), // 11.2 : 1
    onAmber  = Color(0xFF1B1405), // 10.5 : 1 on amber
    amberDim = Color(0xFF3B2F13),

    red    = Color(0xFFFF6B60), //  7.0 : 1
    onRed  = Color(0xFF1F0705), //  6.9 : 1 on red
    redDim = Color(0xFF3D1C19),

    glassFill       = Color.White.copy(alpha = 0.10f),
    glassFillStrong = Color.White.copy(alpha = 0.16f),
    glassStroke     = Color.White.copy(alpha = 0.22f),
    glassHairline   = Color.White.copy(alpha = 0.08f),

    glassFillOpaque       = Color(0xFF1E2127),
    glassFillStrongOpaque = Color(0xFF262A31),

    canvasFalloff = Color(0x000B0D10), // dark needs no falloff; the sheen carries it
    mediaBackdrop = Color(0xFF000000),

    isDark = true,
)

val DnLightColors = DnColors(
    canvas       = Color(0xFFF5F7F3),
    surface      = Color(0xFFFFFFFF),
    surface2     = Color(0xFFECEFE8),
    surface3     = Color(0xFFDFE4D8),
    inverse      = Color(0xFF11141A),

    strokeSubtle = Color(0xFFE2E6DC),
    strokeStrong = Color(0xFFC7CDBF),

    textPrimary   = Color(0xFF11141A), // 17.1 : 1
    textSecondary = Color(0xFF4A535E), //  7.2 : 1
    textTertiary  = Color(0xFF6C7681), //  4.3 : 1
    textInverse   = Color(0xFFFFFFFF),

    // a lime dark enough to carry white text is no longer lime, so the fill
    // keeps dark ink in both modes and only the ink variant changes
    lime    = Color(0xFF9BD62F),
    onLime  = Color(0xFF12200A), // 10.8 : 1 on lime
    limeDim = Color(0xFFE4F4C8),
    limeInk = Color(0xFF3F6E0A), //  5.6 : 1

    blue    = Color(0xFF1660D8), //  5.3 : 1
    onBlue  = Color(0xFFFFFFFF), //  5.7 : 1 on blue
    blueDim = Color(0xFFDCEAFD),

    amber    = Color(0xFF9A6800), //  4.5 : 1
    onAmber  = Color(0xFFFFFFFF), //  4.8 : 1 on amber
    amberDim = Color(0xFFFAEBD0),

    red    = Color(0xFFC8382C), //  4.8 : 1
    onRed  = Color(0xFFFFFFFF), //  5.2 : 1 on red
    redDim = Color(0xFFFBDFDC),

    glassFill       = Color.White.copy(alpha = 0.55f),
    glassFillStrong = Color.White.copy(alpha = 0.72f),
    glassStroke     = Color.White.copy(alpha = 0.90f),
    glassHairline   = Color(0xFF11141A).copy(alpha = 0.08f),

    glassFillOpaque       = Color(0xFFFAFBF9),
    glassFillStrongOpaque = Color(0xFFFFFFFF),

    canvasFalloff = Color(0x8CD4DCC9), // 55 % of #D4DCC9
    mediaBackdrop = Color(0xFF000000),

    isDark = false,
)

val LocalDnColors = staticCompositionLocalOf { DnDarkColors }

/** Shorthand: `DnTheme.colors.lime` */
object DnTheme {
    val colors: DnColors
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalDnColors.current
}
