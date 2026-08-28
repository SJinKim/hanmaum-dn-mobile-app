package com.hanmaum.dn.mobile.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberUrlLauncher(): (String) -> Unit = remember {
    { url ->
        val nsUrl = NSURL(string = url)
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}
