package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.hanmaum.dn.mobile.core.domain.model.ThemeMode

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val colorScheme = if (useDark) WarmPremiumDarkColorScheme else WarmPremiumLightColorScheme
    val typography = rememberAppTypography()
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = AppShapes,
        content     = content,
    )
}
