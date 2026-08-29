package com.hanmaum.dn.mobile.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.navigation.TopLevelDestination
import com.hanmaum.dn.mobile.core.presentation.glass.GlassLevel
import com.hanmaum.dn.mobile.core.presentation.glass.dnGlass
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/**
 * The dock's geometry, in one place so screens can reserve exactly the space
 * it occupies instead of guessing at a magic number.
 */
object DnDock {
    /** Height of the pill itself. */
    val Height = 76.dp

    /** Gap between the pill and the bottom of the safe area. */
    val Gap = 22.dp

    /**
     * Space a scrolling screen must leave below its last item.
     *
     * Includes the platform navigation-bar inset, because the dock sits above
     * it: on a device with a home indicator or a three-button bar the dock is
     * pushed further up the screen, and a fixed reservation left the last row
     * of content underneath it.
     */
    @Composable
    fun contentInset(extra: Dp = 32.dp): Dp =
        Height + Gap + extra +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * Floating dock: a rounded bar that hovers over the content rather than
 * sitting on a solid strip at the bottom. Content scrolls underneath it and
 * fades out through [DnScrollEdge] just before it reaches the bar.
 *
 * Icon over label, and the active state fills a pill *behind* the icon so
 * every tab keeps the same width — with the label inside the pill the active
 * tab grew and the others shifted on every switch.
 */
@Composable
fun BottomNavBar(
    currentDestination: NavDestination?,
    onDestinationSelected: (TopLevelDestination<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val c = DnTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The dock owns its own bottom spacing so it can sit above the
            // system navigation bar rather than underneath it.
            .navigationBarsPadding()
            .padding(bottom = DnDock.Gap)
            .padding(horizontal = 20.dp)
            .height(DnDock.Height)
            .clip(RoundedCornerShape(34.dp))
            .dnGlass(shape = RoundedCornerShape(34.dp), level = GlassLevel.Strong)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevelDestination.all.forEach { dest ->
            val selected = currentDestination?.hasRoute(dest.routeClass) == true
            val label = when (dest) {
                is TopLevelDestination.Home -> strings.navHome
                is TopLevelDestination.News -> strings.navNews
                is TopLevelDestination.Calendar -> strings.navCalendar
                is TopLevelDestination.Album -> strings.navAlbum
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onDestinationSelected(dest) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    Modifier
                        .size(width = 46.dp, height = 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) c.lime else Color.Transparent),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = label,
                        tint = if (selected) c.onLime else c.textSecondary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Text(
                    text = label,
                    style = DnTheme.typography.label,
                    color = if (selected) c.limeInk else c.textSecondary,
                )
            }
        }
    }
}
