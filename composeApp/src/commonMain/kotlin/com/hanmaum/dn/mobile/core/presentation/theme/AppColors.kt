package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Light Mode Surfaces ───────────────────────────────────────────────────────
val LightSurface                = Color(0xFFFDF8F4)
val LightSurfaceContainerLow    = Color(0xFFFAF3ED)
val LightSurfaceContainer       = Color(0xFFF5EBE0)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightOnSurface              = Color(0xFF2C1A0E)
val LightOnSurfaceVariant       = Color(0xFF5A3A28)
val LightMuted                  = Color(0xFFC4A882)
val LightOutlineVariant         = Color(0x26C4A882)

// ── Dark Mode Surfaces ────────────────────────────────────────────────────────
val DarkSurface                 = Color(0xFF1A1208)
val DarkSurfaceContainerLow     = Color(0xFF120D05)
val DarkSurfaceContainer        = Color(0xFF221508)
val DarkSurfaceContainerLowest  = Color(0xFF0D0905)
val DarkOnSurface               = Color(0xFFF5E6CC)
val DarkOnSurfaceVariant        = Color(0xFFC4A070)
val DarkMuted                   = Color(0xFF8A6A3A)
val DarkOutlineVariant          = Color(0x2E8A6A3A)

// ── CI Placeholder — swap when brand colours are confirmed ────────────────────
val LightPrimary     = Color(0xFFC07A50)
val LightPrimaryDark = Color(0xFF8A4A28)
val LightOnPrimary   = Color(0xFFFFFFFF)

val DarkPrimary      = Color(0xFFA0622A)
val DarkPrimaryDark  = Color(0xFF6A3A10)
val DarkOnPrimary    = Color(0xFFFDE8C0)

// ── Floating Pill Nav — always dark-frosted in both light and dark modes ───────
val PillBackground   = Color(0xDD2C1A0E) // DarkOnSurface @ 87% opacity
val PillIndicator    = Color(0x30C4A882) // LightMuted @ 19% opacity — active chip
val PillIconActive   = Color(0xFFC4A882) // LightMuted @ 100% — active icon + label
val PillIconInactive = Color(0x66C4A882) // LightMuted @ 40% — inactive icon

// ── Shared ────────────────────────────────────────────────────────────────────
val ErrorRed         = Color(0xFFBA1A1A)
val ErrorContainer   = Color(0xFFFFDAD6)
val OnError          = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// ── Material3 colour schemes ──────────────────────────────────────────────────
val WarmPremiumLightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightPrimaryDark,
    onPrimaryContainer   = LightOnPrimary,
    secondary            = LightMuted,
    onSecondary          = LightOnSurface,
    secondaryContainer   = LightSurfaceContainerLow,
    onSecondaryContainer = LightOnSurfaceVariant,
    background           = LightSurface,
    onBackground         = LightOnSurface,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceContainerLow,
    onSurfaceVariant     = LightOnSurfaceVariant,
    surfaceContainer            = LightSurfaceContainer,
    surfaceContainerLow         = LightSurfaceContainerLow,
    surfaceContainerLowest      = LightSurfaceContainerLowest,
    outline              = LightMuted,
    outlineVariant       = LightOutlineVariant,
    error                = ErrorRed,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    scrim                = Color(0xFF2C1A0E),
    inverseSurface       = DarkSurface,
    inverseOnSurface     = DarkOnSurface,
    inversePrimary       = DarkPrimary,
)

val WarmPremiumDarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryDark,
    onPrimaryContainer   = DarkOnPrimary,
    secondary            = DarkMuted,
    onSecondary          = DarkOnSurface,
    secondaryContainer   = DarkSurfaceContainerLow,
    onSecondaryContainer = DarkOnSurfaceVariant,
    background           = DarkSurface,
    onBackground         = DarkOnSurface,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceContainerLow,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    surfaceContainer            = DarkSurfaceContainer,
    surfaceContainerLow         = DarkSurfaceContainerLow,
    surfaceContainerLowest      = DarkSurfaceContainerLowest,
    outline              = DarkMuted,
    outlineVariant       = DarkOutlineVariant,
    error                = ErrorRed,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    scrim                = Color(0xFF000000),
    inverseSurface       = LightSurface,
    inverseOnSurface     = LightOnSurface,
    inversePrimary       = LightPrimary,
)
