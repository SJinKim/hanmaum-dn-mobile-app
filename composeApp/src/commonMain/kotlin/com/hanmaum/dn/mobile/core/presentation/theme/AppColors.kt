package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material3 schemes derived from [DnColors] so stock Material components
 * (text fields, sheets, ripples) land on the same palette. Anything the
 * redesign draws itself reads [DnColors] directly — Material has no slot
 * for the glass material or for the accent containers.
 */
fun DnColors.toMaterialColorScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary            = lime,
            onPrimary          = onLime,
            primaryContainer   = limeDim,
            onPrimaryContainer = limeInk,
            secondary          = blue,
            onSecondary        = onBlue,
            secondaryContainer = blueDim,
            onSecondaryContainer = blue,
            tertiary           = amber,
            onTertiary         = onAmber,
            tertiaryContainer  = amberDim,
            onTertiaryContainer = amber,
            error              = red,
            onError            = onRed,
            errorContainer     = redDim,
            onErrorContainer   = red,
            background         = canvas,
            onBackground       = textPrimary,
            surface            = surface,
            onSurface          = textPrimary,
            surfaceVariant     = surface2,
            onSurfaceVariant   = textSecondary,
            surfaceContainerLowest = canvas,
            surfaceContainerLow    = surface,
            surfaceContainer       = surface2,
            surfaceContainerHigh   = surface3,
            outline            = strokeStrong,
            outlineVariant     = strokeSubtle,
            inverseSurface     = inverse,
            inverseOnSurface   = textInverse,
            scrim              = Color.Black,
        )
    } else {
        lightColorScheme(
            primary            = lime,
            onPrimary          = onLime,
            primaryContainer   = limeDim,
            onPrimaryContainer = limeInk,
            secondary          = blue,
            onSecondary        = onBlue,
            secondaryContainer = blueDim,
            onSecondaryContainer = blue,
            tertiary           = amber,
            onTertiary         = onAmber,
            tertiaryContainer  = amberDim,
            onTertiaryContainer = amber,
            error              = red,
            onError            = onRed,
            errorContainer     = redDim,
            onErrorContainer   = red,
            background         = canvas,
            onBackground       = textPrimary,
            surface            = surface,
            onSurface          = textPrimary,
            surfaceVariant     = surface2,
            onSurfaceVariant   = textSecondary,
            surfaceContainerLowest = surface,
            surfaceContainerLow    = canvas,
            surfaceContainer       = surface2,
            surfaceContainerHigh   = surface3,
            outline            = strokeStrong,
            outlineVariant     = strokeSubtle,
            inverseSurface     = inverse,
            inverseOnSurface   = textInverse,
            scrim              = Color.Black,
        )
    }
