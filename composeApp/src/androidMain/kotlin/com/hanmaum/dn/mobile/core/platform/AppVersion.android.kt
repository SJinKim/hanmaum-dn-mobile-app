package com.hanmaum.dn.mobile.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hanmaum.dn.mobile.BuildConfig

@Composable
internal actual fun rememberAppVersion(): String? = remember<String?> {
    displayAppVersion(BuildConfig.VERSION_NAME)
}
