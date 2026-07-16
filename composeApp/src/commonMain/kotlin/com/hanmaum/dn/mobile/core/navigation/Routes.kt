package com.hanmaum.dn.mobile.core.navigation

import kotlinx.serialization.Serializable

@Serializable object SplashRoute
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object PendingRoute
@Serializable object HomeRoute
@Serializable object AnnouncementListRoute
@Serializable data class AnnouncementDetailRoute(val id: String)
@Serializable object ProfileRoute
@Serializable object PersonalInfoRoute
@Serializable object SettingsRoute
@Serializable object MinistryListRoute
@Serializable data class MinistryDetailRoute(val publicId: String)
@Serializable object CommunityRoute
@Serializable object FloorPlanRoute
@Serializable object AttendanceRoute
@Serializable object AlbumsRoute
@Serializable data class AlbumDetailRoute(val pcloudCode: String, val albumName: String)
@Serializable data class PhotoViewerRoute(val photoUrl: String)
@Serializable object CalendarRoute
@Serializable object NotificationListRoute
