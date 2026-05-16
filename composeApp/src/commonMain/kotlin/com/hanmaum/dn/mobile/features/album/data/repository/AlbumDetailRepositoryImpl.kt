package com.hanmaum.dn.mobile.features.album.data.repository

import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.features.album.data.model.PCloudDownloadResponse
import com.hanmaum.dn.mobile.features.album.data.model.PCloudFolderResponse
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumDetailRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class AlbumDetailRepositoryImpl(private val client: HttpClient) : AlbumDetailRepository {

    private val folderEndpoint = BuildKonfig.PCLOUD_FOLDER_ENDPOINT
    private val downloadEndpoint = BuildKonfig.PCLOUD_DOWNLOAD_ENDPOINT

    override suspend fun getFolderContents(pcloudCode: String): Result<List<AlbumItem>> = runCatching {
        val response = client.get("$folderEndpoint?code=$pcloudCode")
        val body = response.body<PCloudFolderResponse>()
        if (body.result != 0) error("pCloud error code ${body.result}")
        body.metadata?.contents
            ?.filter { !it.isfolder && it.fileid != null && isImageFile(it.name) }
            ?.map { AlbumItem(fileId = it.fileid!!, name = it.name, sizeBytes = it.size ?: 0L) }
            ?: emptyList()
    }

    override suspend fun getDownloadUrl(pcloudCode: String, fileId: Long): Result<String> = runCatching {
        val response = client.get("$downloadEndpoint?code=$pcloudCode&fileid=$fileId")
        val body = response.body<PCloudDownloadResponse>()
        if (body.result != 0) error("pCloud download error code ${body.result}")
        if (body.hosts.isEmpty()) error("pCloud download returned no hosts (result=${body.result})")
        "https://${body.hosts[0]}${body.path}"
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".webp")
    }
}
