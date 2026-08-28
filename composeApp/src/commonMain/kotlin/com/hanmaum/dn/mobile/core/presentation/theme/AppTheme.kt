package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The redesign is dark-first but ships both modes from one source, exactly as
 * the Figma file does: the same layout, a different set of colour values.
 *
 * @param darkTheme defaults to the system setting; pass a fixed value to
 *   preview a single mode.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dnColors = if (darkTheme) DnDarkColors else DnLightColors
    val dnTypography = rememberDnTypography()

    CompositionLocalProvider(
        LocalDnColors provides dnColors,
        LocalDnTypography provides dnTypography,
    ) {
        MaterialTheme(
            colorScheme = dnColors.toMaterialColorScheme(),
            typography  = dnTypography.toMaterialTypography(),
            shapes      = AppShapes,
            content     = content,
        )
    }
}
