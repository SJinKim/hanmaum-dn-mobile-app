package com.hanmaum.dn.mobile.features.announcement.data.repository

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode


class AnnouncementRepositoryImpl(
    private val client: HttpClient
) : AnnouncementRepository {
    override suspend fun getAnnouncements(): List<Announcement> {
        return try {
            val response = client.get("announcements")
            if (response.status != HttpStatusCode.OK) {
                emptyList()
            } else {
                response.body()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    override suspend fun getAnnouncementById(id: String): Announcement? {
        val all = getAnnouncements()
        return all.find { it.id == id }
    }
}