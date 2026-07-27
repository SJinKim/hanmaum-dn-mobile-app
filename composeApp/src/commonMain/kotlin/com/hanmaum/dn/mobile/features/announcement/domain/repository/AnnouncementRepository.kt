package com.hanmaum.dn.mobile.features.announcement.domain.repository

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.model.AnnouncementLookup

interface AnnouncementRepository {
    suspend fun getAnnouncements(): List<Announcement>
    suspend fun getAnnouncementById(id: String): AnnouncementLookup
}