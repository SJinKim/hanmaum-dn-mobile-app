package com.hanmaum.dn.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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

    data object Community : TopLevelDestination<CommunityRoute>(
        routeClass = CommunityRoute::class,
        routeInstance = CommunityRoute,
        icon = Icons.Default.Group,
        label = "커뮤니티",
    )

    data object Ministries : TopLevelDestination<MinistryListRoute>(
        routeClass = MinistryListRoute::class,
        routeInstance = MinistryListRoute,
        icon = Icons.Default.Star,
        label = "사역",
    )

    data object News : TopLevelDestination<AnnouncementListRoute>(
        routeClass = AnnouncementListRoute::class,
        routeInstance = AnnouncementListRoute,
        icon = Icons.Default.Newspaper,
        label = "소식",
    )

    data object Profile : TopLevelDestination<ProfileRoute>(
        routeClass = ProfileRoute::class,
        routeInstance = ProfileRoute,
        icon = Icons.Default.Person,
        label = "프로필",
    )

    companion object {
        val all: List<TopLevelDestination<*>> = listOf(Home, Community, Ministries, News, Profile)
    }
}
