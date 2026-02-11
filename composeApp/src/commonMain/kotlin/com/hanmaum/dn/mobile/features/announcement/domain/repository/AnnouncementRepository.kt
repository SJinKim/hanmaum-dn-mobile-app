package com.hanmaum.dn.mobile.features.announcement.domain.repository

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement

interface AnnouncementRepository {
    suspend fun getAnnouncements(): List<Announcement>
    suspend fun getAnnouncementById(id: String): Announcement?
}