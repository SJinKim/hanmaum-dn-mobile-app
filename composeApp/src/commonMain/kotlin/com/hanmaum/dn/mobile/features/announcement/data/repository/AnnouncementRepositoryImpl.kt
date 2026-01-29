package com.hanmaum.dn.mobile.features.announcement.data.repository

import com.hanmaum.dn.mobile.core.network.NetworkClient
import com.hanmaum.dn.mobile.core.util.AppConfig
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AnnouncementRepositoryImpl : AnnouncementRepository {
    private val client = NetworkClient.client

    override suspend fun getAnnouncements(token: String): List<Announcement> {
        val url = "${AppConfig.getBackendUrl()}/announcements"
        val response = client.get(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        return response.body()

    }

    override suspend fun getAnnouncementById(token: String, id: String): Announcement? {
        val all = getAnnouncements(token)
        return all.find { it.id == id }
    }
}