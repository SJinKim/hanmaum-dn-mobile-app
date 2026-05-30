package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.hanmaum.dn.mobile.core.navigation.TopLevelDestination
import com.hanmaum.dn.mobile.core.presentation.theme.AppMotion
import kotlin.math.roundToInt

@Composable
fun FloatingPillNav(
    currentDestination: NavDestination?,
    onDestinationSelected: (TopLevelDestination<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = TopLevelDestination.all
    val selectedIndex = remember(currentDestination) {
        destinations.indexOfFirst { dest ->
            currentDestination?.hasRoute(dest.routeClass) == true
        }.coerceAtLeast(0)
    }

    val density = LocalDensity.current
    val bottomInsetPx = WindowInsets.navigationBars.getBottom(density)
    val bottomInsetDp = with(density) { bottomInsetPx.toDp() }

    var pillWidthPx by remember { mutableIntStateOf(0) }
    val itemWidthPx = if (destinations.isNotEmpty()) pillWidthPx / destinations.size else 0

    val indicatorOffsetPx by animateFloatAsState(
        targetValue = (selectedIndex * itemWidthPx).toFloat(),
        animationSpec = AppMotion.pillIndicator,
        label = "pillIndicator",
    )

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = bottomInsetDp + 16.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xDD2C1A0E))
            .onSizeChanged { pillWidthPx = it.width },
    ) {
        if (itemWidthPx > 0) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                    .width(with(density) { itemWidthPx.toDp() })
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0x30C4A882)),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            destinations.forEachIndexed { index, dest ->
                PillNavItem(
                    destination = dest,
                    selected = index == selectedIndex,
                    onClick = { onDestinationSelected(dest) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun PillNavItem(
    destination: TopLevelDestination<*>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                fadeIn(AppMotion.pillIndicator) togetherWith fadeOut(AppMotion.pillIndicator)
            },
            label = "pillItemContent_${destination.label}",
        ) { isSelected ->
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = Color(0xFFC4A882),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFC4A882),
                    )
                }
            } else {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    tint = Color(0x66C4A882),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
