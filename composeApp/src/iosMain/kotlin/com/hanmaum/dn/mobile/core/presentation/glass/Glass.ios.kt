package com.hanmaum.dn.mobile.core.presentation.glass

/**
 * iOS could carry the real material through `UIVisualEffectView` interop, but
 * it stays off until Android can match it — otherwise the two platforms drift
 * apart visually. Flip this once the shared implementation lands.
 */
actual fun backdropBlurSupported(): Boolean = false
