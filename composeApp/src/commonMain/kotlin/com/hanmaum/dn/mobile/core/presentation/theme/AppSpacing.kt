package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.ui.unit.dp

// Design spec §10 — Spacing
// Base unit is 4dp; every spacing value is a multiple of it.
// Screens must use these tokens instead of literal dp — literal dp in a screen file
// is what let horizontal padding drift to 4/8/10/16/20/24/28/32dp across the app.
object AppSpacing {
    /** Icon-to-label gap, badge padding */
    val xs = 4.dp

    /** Between related elements, mini-card gap */
    val sm = 8.dp

    /** Horizontal screen padding — always this on both sides. Also the card gap. */
    val md = 16.dp

    /** Between content sections */
    val lg = 24.dp

    /** Above major headings, between feature blocks */
    val xl = 32.dp

    /** Bottom padding required to clear the floating pill nav */
    val bottomNav = 80.dp
}
