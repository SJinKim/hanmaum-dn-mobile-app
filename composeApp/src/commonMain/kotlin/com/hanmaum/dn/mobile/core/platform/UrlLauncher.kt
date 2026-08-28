package com.hanmaum.dn.mobile.core.platform

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens [url] in the system browser.
 * Used to hand off password reset to Keycloak's hosted reset-credentials page.
 */
@Composable
expect fun rememberUrlLauncher(): (String) -> Unit
