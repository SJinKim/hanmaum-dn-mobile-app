package com.hanmaum.dn.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VolunteerActivism
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

    data object Ministry : TopLevelDestination<MinistryListRoute>(
        routeClass = MinistryListRoute::class,
        routeInstance = MinistryListRoute,
        icon = Icons.Default.VolunteerActivism,
        label = "사역",
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
        val all: List<TopLevelDestination<*>> = listOf(Home, Ministry, Calendar, Album, Profile)
    }
}
