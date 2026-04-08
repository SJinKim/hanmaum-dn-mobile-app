package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Brand Tokens ─────────────────────────────────────────────────────────────
// Source: designs/design_md/DESIGN.md — "The Luminous Sanctuary"

// Primary: Coral — energy, passion, primary actions
val CoralDark        = Color(0xFFAE2F34)
val CoralLight       = Color(0xFFFF6B6B)
val OnCoral          = Color(0xFFFFFFFF)
val OnCoralContainer = Color(0xFF410002)

// Secondary: Faith Blue — grounding, navigation
val BlueDark         = Color(0xFF005DB8)
val BlueLight        = Color(0xFF4C96FE)
val OnBlue           = Color(0xFFFFFFFF)
val OnBlueContainer  = Color(0xFF001C3B)

// Tertiary: Holy Gold — "Aha!" moments, highlights
val GoldDark         = Color(0xFF705D00)
val GoldLight        = Color(0xFFFFE173)
val OnGold           = Color(0xFFFFFFFF)
val OnGoldContainer  = Color(0xFF221B00)

// Surface hierarchy (tonal layering — no borders)
val SanctuaryWhite   = Color(0xFFF9F9F9)  // base background — never pure white
val CardWhite        = Color(0xFFFFFFFF)  // cards on top of SanctuaryWhite
val SurfaceLow       = Color(0xFFF3F3F3)  // sectioning shift (surface_container_low)
val SurfaceMid       = Color(0xFFEEEEEE)  // heavier inset

// Text — warm charcoal (never pure black per design spec)
val DeepCharcoal     = Color(0xFF2D3436)  // primary text
val WarmCharcoal     = Color(0xFF584140)  // body text / on_surface_variant
val MutedGray        = Color(0xFF857371)  // outline / inactive

// Special
val SoftPeach        = Color(0xFFFFF5E1)  // reading section bg

// Error
val ErrorRed         = Color(0xFFBA1A1A)
val ErrorContainer   = Color(0xFFFFDAD6)
val OnError          = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// ── Material3 ColorScheme ─────────────────────────────────────────────────────
val LuminousSanctuaryColorScheme = lightColorScheme(
    primary                = CoralDark,
    onPrimary              = OnCoral,
    primaryContainer       = CoralLight,
    onPrimaryContainer     = OnCoralContainer,
    secondary              = BlueDark,
    onSecondary            = OnBlue,
    secondaryContainer     = BlueLight,
    onSecondaryContainer   = OnBlueContainer,
    tertiary               = GoldDark,
    onTertiary             = OnGold,
    tertiaryContainer      = GoldLight,
    onTertiaryContainer    = OnGoldContainer,
    error                  = ErrorRed,
    onError                = OnError,
    errorContainer         = ErrorContainer,
    onErrorContainer       = OnErrorContainer,
    background             = SanctuaryWhite,
    onBackground           = DeepCharcoal,
    surface                = SanctuaryWhite,
    onSurface              = DeepCharcoal,
    surfaceVariant         = SurfaceLow,
    onSurfaceVariant       = WarmCharcoal,
    outline                = MutedGray,
    outlineVariant         = Color(0xFFD8C2BF),
    scrim                  = Color(0xFF000000),
    inverseSurface         = Color(0xFF362F2E),
    inverseOnSurface       = SoftPeach,
    inversePrimary         = Color(0xFFFFB3AE),
    surfaceTint            = CoralDark,
)
