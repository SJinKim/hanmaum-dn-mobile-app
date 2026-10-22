package com.hanmaum.dn.mobile.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun ProvidePlatformLanguage(languageTag: String, content: @Composable () -> Unit) {
    // Compose's Locale.current and Material controls read NSLocale.preferredLanguages.
    // Apply before composing children; a SideEffect would leave their first frame in the old language.
    remember(languageTag) {
        NSUserDefaults.standardUserDefaults.setObject(listOf(languageTag), "AppleLanguages")
    }
    content()
}
