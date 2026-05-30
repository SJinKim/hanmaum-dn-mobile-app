package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Design spec §4 — Shape & Corner Radius
// shape_small   → 6dp   inputs, chips, badges
// shape_medium  → 14dp  list cards, modals
// shape_large   → 20dp  hero cards, bottom sheets
// shape_full    → ∞     buttons, pill nav, avatars
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(percent = 50), // pill / shape_full
    small      = RoundedCornerShape(6.dp),         // shape_small
    medium     = RoundedCornerShape(14.dp),        // shape_medium
    large      = RoundedCornerShape(20.dp),        // shape_large
    extraLarge = RoundedCornerShape(20.dp),        // also shape_large for sheets
)
