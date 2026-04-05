package com.hanmaum.dn.mobile.core.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

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

@Serializable
object ProfileRoute

@Serializable
object MinistryListRoute

@Serializable
data class MinistryDetailRoute(val publicId: String)