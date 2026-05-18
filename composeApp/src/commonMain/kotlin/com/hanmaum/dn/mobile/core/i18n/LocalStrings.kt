package com.hanmaum.dn.mobile.core.i18n

import androidx.compose.runtime.compositionLocalOf

// Intentional: compositionLocalOf (not static) so language changes trigger full recomposition
val LocalStrings = compositionLocalOf<AppStrings> { EnStrings }
