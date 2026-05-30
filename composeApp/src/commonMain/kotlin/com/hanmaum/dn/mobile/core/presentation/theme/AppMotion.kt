package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.animation.core.spring

// Design spec §8 — Motion / Hybrid Spring System
// Never use LinearEasing or EaseInOut. Always use spring().
object AppMotion {
    /** Card → Detail shared element expand */
    val cardToDetail = spring<Float>(dampingRatio = 0.75f, stiffness = 200f)

    /** Floating pill indicator slide between tabs */
    val pillIndicator = spring<Float>(dampingRatio = 0.85f, stiffness = 280f)

    /** Screen push (slide from right / bottom sheet slide up) */
    val screenPush = spring<Float>(dampingRatio = 0.80f, stiffness = 250f)

    /** Button / card press scale 1.0 → 0.97 → 1.0 */
    val press = spring<Float>(dampingRatio = 0.60f, stiffness = 400f)

    /** List item stagger fade + 8dp Y slide */
    val listItem = spring<Float>(dampingRatio = 0.85f, stiffness = 260f)

    /** Press target scale */
    const val PRESS_SCALE = 0.97f

    /** Stagger delay per list item in milliseconds (max 5 items) */
    const val STAGGER_DELAY_MS = 40
}
