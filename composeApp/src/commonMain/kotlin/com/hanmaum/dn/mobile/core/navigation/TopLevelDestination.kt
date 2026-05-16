package com.hanmaum.dn.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

sealed class TopLevelDestination<T : Any>(
    val routeClass: KClass<T>,
    val routeInstance: T,
    val icon: ImageVector,
    val label: String,
) {
    data object Home : TopLevelDestination<HomeRoute>(
        routeClass = HomeRoute::class,
        routeInstance = HomeRoute,
        icon = Icons.Default.Home,
        label = "홈",
    )

    data object News : TopLevelDestination<AnnouncementListRoute>(
        routeClass = AnnouncementListRoute::class,
        routeInstance = AnnouncementListRoute,
        icon = Icons.Default.Newspaper,
        label = "소식",
    )

    data object Calendar : TopLevelDestination<CalendarRoute>(
        routeClass = CalendarRoute::class,
        routeInstance = CalendarRoute,
        icon = Icons.Default.CalendarMonth,
        label = "캘린더",
    )

    data object Album : TopLevelDestination<AlbumsRoute>(
        routeClass = AlbumsRoute::class,
        routeInstance = AlbumsRoute,
        icon = Icons.Default.PhotoLibrary,
        label = "앨범",
    )

    data object Profile : TopLevelDestination<ProfileRoute>(
        routeClass = ProfileRoute::class,
        routeInstance = ProfileRoute,
        icon = Icons.Default.Person,
        label = "프로필",
    )

    companion object {
        val all: List<TopLevelDestination<*>> = listOf(Home, News, Calendar, Album, Profile)
    }
}
