package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) {
        WarmPremiumDarkColorScheme
    } else {
        WarmPremiumLightColorScheme
    }
    val typography = rememberAppTypography()
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = AppShapes,
        content     = content,
    )
}
