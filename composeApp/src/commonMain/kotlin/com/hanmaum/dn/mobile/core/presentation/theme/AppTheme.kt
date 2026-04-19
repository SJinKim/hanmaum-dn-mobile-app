package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val typography = rememberAppTypography()
    MaterialTheme(
        colorScheme = LuminousSanctuaryColorScheme,
        typography  = typography,
        shapes      = AppShapes,
        content     = content,
    )
}
