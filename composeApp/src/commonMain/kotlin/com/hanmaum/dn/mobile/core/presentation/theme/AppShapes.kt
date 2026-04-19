package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Design spec: "The Luminous Sanctuary"
// Sharp 90° corners are strictly prohibited — everything must feel soft and approachable.
//
//   Buttons / chips / badges → pill (rounded-full)        extraSmall
//   Inputs / small surfaces  → rounded-md  ≈ 12dp         small
//   Internal cards           → rounded-lg  ≈ 20dp         medium
//   Container cards          → rounded-xl  ≈ 24dp         large   (default Card shape)
//   Hero / feature / sheets  → extra-large  32dp          extraLarge

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(percent = 50), // Pill
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(20.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
