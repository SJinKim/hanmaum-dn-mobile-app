package com.hanmaum.dn.mobile.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Four radii, matching the Figma collection:
 *
 *   inner 18  inputs, small tiles
 *   tile  24  cards inside a list, icon tiles
 *   card  28  top-level cards, sheets
 *   pill      anything interactive — buttons, chips, the dock
 */
object DnRadius {
    val inner = 18.dp
    val tile  = 24.dp
    val card  = 28.dp
    val pill  = 999.dp
}

val DnInnerShape = RoundedCornerShape(DnRadius.inner)
val DnTileShape  = RoundedCornerShape(DnRadius.tile)
val DnCardShape  = RoundedCornerShape(DnRadius.card)
val DnPillShape  = RoundedCornerShape(percent = 50)

val AppShapes = Shapes(
    extraSmall = DnPillShape,
    small      = DnInnerShape,
    medium     = DnTileShape,
    large      = DnCardShape,
    extraLarge = RoundedCornerShape(32.dp),
)
