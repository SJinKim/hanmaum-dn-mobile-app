package com.hanmaum.dn.mobile.features.announcement.data.repository

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*


class AnnouncementRepositoryImpl(
    private val client: HttpClient
) : AnnouncementRepository {

    override suspend fun getAnnouncements(): List<Announcement> {
        val announcementUrl = "announcements"
        return client.get(announcementUrl).body()

    }

    override suspend fun getAnnouncementById(id: String): Announcement? {
        val all = getAnnouncements()
        return all.find { it.id == id }
    }
}