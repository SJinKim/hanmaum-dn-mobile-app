package com.hanmaum.dn.mobile.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import kotlin.reflect.KClass

/**
 * Four tabs, not five.
 *
 * Profile left the tab bar in the v2 redesign and is reached through the
 * avatar in the Home header instead — it is not a frequent destination, and
 * dropping it gives the remaining four tabs noticeably larger touch targets.
 */
sealed class TopLevelDestination<T : Any>(
    val routeClass: KClass<T>,
    val routeInstance: T,
    val icon: ImageVector,
) {
    data object Home : TopLevelDestination<HomeRoute>(
        routeClass = HomeRoute::class,
        routeInstance = HomeRoute,
        icon = DnIcons.Home,
    )

    data object News : TopLevelDestination<AnnouncementListRoute>(
        routeClass = AnnouncementListRoute::class,
        routeInstance = AnnouncementListRoute,
        icon = DnIcons.News,
    )

    data object Calendar : TopLevelDestination<CalendarRoute>(
        routeClass = CalendarRoute::class,
        routeInstance = CalendarRoute,
        icon = DnIcons.Calendar,
    )

    data object Album : TopLevelDestination<AlbumsRoute>(
        routeClass = AlbumsRoute::class,
        routeInstance = AlbumsRoute,
        icon = DnIcons.Image,
    )

    companion object {
        val all: List<TopLevelDestination<*>> = listOf(Home, News, Calendar, Album)
    }
}
