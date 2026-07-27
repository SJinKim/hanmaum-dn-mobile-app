package com.hanmaum.dn.mobile.features.announcement.domain.model

/**
 * Typed outcome of looking up a single announcement by id, so the ViewModel can tell a
 * genuinely-missing announcement (expired/removed — no retry helps) apart from a
 * transient network failure (retryable). Decouples presentation from Ktor/HTTP.
 */
sealed interface AnnouncementLookup {
    data class Found(val announcement: Announcement) : AnnouncementLookup
    data object NotFound : AnnouncementLookup // fetched OK, id absent from the active feed
    data object Error : AnnouncementLookup // network / non-2xx failure
}
