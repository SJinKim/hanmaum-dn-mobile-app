package com.hanmaum.dn.mobile.core.presentation.glass

/**
 * Android could blur a backdrop from API 31 via `RenderEffect`, but there is
 * no common-code path to it yet and older devices could not follow. Until a
 * real implementation exists the app uses the opaque stand-in on every device,
 * so the look is identical across the whole install base instead of splitting
 * into two visual tiers.
 */
actual fun backdropBlurSupported(): Boolean = false
