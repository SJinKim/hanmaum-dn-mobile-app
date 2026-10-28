package com.hanmaum.dn.mobile.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle

@Composable
internal actual fun rememberAppVersion(): String? = remember<String?> {
    displayAppVersion(NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
}
