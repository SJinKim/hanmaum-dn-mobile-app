package com.hanmaum.dn.mobile.core.navigation

import kotlinx.serialization.Serializable

@Serializable object SplashRoute
/**
 * [notice] is a code, never a sentence: routes are persisted across process
 * death and a literal message would be frozen in the language it was written
 * in. The screen resolves it through LocalStrings.
 */
@Serializable data class LoginRoute(val notice: String? = null) {
    companion object {
        /** Registered, but the email must be confirmed before signing in. */
        const val NOTICE_VERIFY_EMAIL = "VERIFY_EMAIL"
        /** Registered; the automatic sign-in did not go through. */
        const val NOTICE_REGISTERED = "REGISTERED"
    }
}
@Serializable object RegisterRoute
@Serializable object PendingRoute
@Serializable object RejectedRoute
@Serializable object HomeRoute
@Serializable object AnnouncementListRoute
@Serializable data class AnnouncementDetailRoute(val id: String)
@Serializable object ProfileRoute
@Serializable object MinistryListRoute
@Serializable data class MinistryDetailRoute(val publicId: String)
@Serializable object CommunityRoute
@Serializable object FloorPlanRoute
@Serializable object AttendanceRoute
@Serializable object AttendanceHistoryRoute
@Serializable object RsvpRoute
@Serializable object AlbumsRoute
@Serializable data class AlbumDetailRoute(val pcloudCode: String, val albumName: String)
@Serializable data class PhotoViewerRoute(val photoUrl: String)
@Serializable object CalendarRoute
@Serializable object NotificationsRoute
@Serializable object SettingsRoute

/**
 * 양육 and 사역 share one screen with a segmented toggle; the tab decides
 * which list opens. Two quick-menu chips on Home point at the same route
 * with different tabs.
 */
@Serializable data class ParticipationRoute(val tab: String = TAB_NURTURE) {
    companion object {
        const val TAB_NURTURE = "NURTURE"
        const val TAB_SERVE = "SERVE"
    }
}

@Serializable data class NurtureDetailRoute(val publicId: String)
