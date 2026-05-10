package com.hanmaum.dn.mobile.features.album.data.repository

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.features.album.data.model.PCloudDownloadResponse
import com.hanmaum.dn.mobile.features.album.data.model.PCloudFolderResponse
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val PCLOUD_API = "https://api.pcloud.com"

class AlbumRepositoryImpl(private val client: HttpClient) : AlbumRepository {

    private val code = BuildKonfig.PCLOUD_PUBLIC_CODE

    override suspend fun getFolderContents(): Result<List<AlbumItem>> = runCatching {
        val response = client.get("$PCLOUD_API/getpublinkcontents?code=$code")
        val body = response.body<PCloudFolderResponse>()
        if (body.result != 0) error("pCloud error code ${body.result}")
        body.metadata?.contents
            ?.filter { !it.isfolder && it.fileid != null && isImageFile(it.name) }
            ?.map { AlbumItem(fileId = it.fileid!!, name = it.name, sizeBytes = it.size ?: 0L) }
            ?: emptyList()
    }

    override suspend fun getDownloadUrl(fileId: Long): Result<String> = runCatching {
        val response = client.get("$PCLOUD_API/getpublinkdownload?code=$code&fileid=$fileId")
        val body = response.body<PCloudDownloadResponse>()
        if (body.result != 0 || body.hosts.isEmpty()) error("pCloud download error ${body.result}")
        "https://${body.hosts[0]}${body.path}"
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".webp")
    }
}
