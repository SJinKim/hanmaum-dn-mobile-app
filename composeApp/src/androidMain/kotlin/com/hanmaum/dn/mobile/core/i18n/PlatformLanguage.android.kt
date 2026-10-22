package com.hanmaum.dn.mobile.core.i18n

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
internal actual fun ProvidePlatformLanguage(languageTag: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(configuration, languageTag) {
        Configuration(configuration).apply { setLocale(Locale.forLanguageTag(languageTag)) }
    }
    // Keep the Activity in the wrapper chain for browser and biometric prompts.
    val localizedContext = remember(context, localized) {
        ContextThemeWrapper(context, context.theme).apply { applyOverrideConfiguration(localized) }
    }
    CompositionLocalProvider(LocalConfiguration provides localized, LocalContext provides localizedContext,
        content = content)
}
