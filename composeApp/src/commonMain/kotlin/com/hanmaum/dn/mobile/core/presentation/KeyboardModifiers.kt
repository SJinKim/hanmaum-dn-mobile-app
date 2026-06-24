package com.hanmaum.dn.mobile.core.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Dismisses the soft keyboard (and clears text-field focus) when the user taps
 * anywhere on this composable that isn't an interactive child. Standard form UX:
 * tapping a field opens the keyboard; tapping the surrounding space dismisses it.
 *
 * Implemented with [pointerInput] + [detectTapGestures] rather than `clickable`
 * on purpose: no ripple, no click/focus semantics on the background, and it
 * composes cleanly with a parent `verticalScroll` (scrolling is a drag, dismissal
 * is a tap). [LocalFocusManager.clearFocus] both deselects the field and hides
 * the IME on Android and iOS.
 */
@Composable
fun Modifier.dismissKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return this.pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}
