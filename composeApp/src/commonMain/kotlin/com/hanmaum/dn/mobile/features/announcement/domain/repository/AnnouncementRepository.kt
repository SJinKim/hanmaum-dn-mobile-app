package com.hanmaum.dn.mobile.features.announcement.domain.repository

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement

interface AnnouncementRepository {
    suspend fun getAnnouncements(token: String): List<Announcement>
    suspend fun getAnnouncementById(token: String, id: String): Announcement?
}