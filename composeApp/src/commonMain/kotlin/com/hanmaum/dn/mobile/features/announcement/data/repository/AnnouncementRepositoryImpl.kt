package com.hanmaum.dn.mobile.features.announcement.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.model.AnnouncementLookup
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
                response.body<ApiResponse<List<Announcement>>>().data ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    // Backend has no GET /announcements/{id}; we fetch the active feed and match by id.
    // A dedicated HTTP call (not getAnnouncements(), which swallows errors into an empty
    // list) lets us tell a genuinely-missing announcement from a transient network fault.
    override suspend fun getAnnouncementById(id: String): AnnouncementLookup {
        return try {
            val response = client.get("announcements")
            if (response.status != HttpStatusCode.OK) {
                AnnouncementLookup.Error
            } else {
                val list = response.body<ApiResponse<List<Announcement>>>().data ?: emptyList()
                list.find { it.id == id }
                    ?.let { AnnouncementLookup.Found(it) }
                    ?: AnnouncementLookup.NotFound
            }
        } catch (_: Exception) {
            AnnouncementLookup.Error
        }
    }
}