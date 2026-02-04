package com.hanmaum.dn.mobile.core.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
object PendingRoute

@Serializable
object HomeRoute

@Serializable
object AnnouncementListRoute

@Serializable
data class AnnouncementDetailRoute(val id: String)