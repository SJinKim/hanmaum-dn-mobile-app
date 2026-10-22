package com.hanmaum.dn.mobile.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Localizes library-owned controls without recreating navigation or changing the session. */
@Composable
internal fun ProvideAppLanguage(strings: AppStrings, content: @Composable () -> Unit) {
    ProvidePlatformLanguage(strings.languageTag) {
        CompositionLocalProvider(LocalStrings provides strings, content = content)
    }
}

@Composable
internal expect fun ProvidePlatformLanguage(languageTag: String, content: @Composable () -> Unit)
