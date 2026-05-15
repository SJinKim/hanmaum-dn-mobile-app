package com.hanmaum.dn.mobile.features.album.data.repository

import com.hanmaum.dn.mobile.features.album.data.model.AlbumDto
import com.hanmaum.dn.mobile.features.album.domain.model.Album
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class AlbumsRepositoryImpl(private val client: HttpClient) : AlbumsRepository {
    override suspend fun getAlbums(): Result<List<Album>> = runCatching {
        client.get("albums")
            .body<List<AlbumDto>>()
            .sortedBy { it.displayOrder }
            .map { Album(publicId = it.publicId, name = it.name, pcloudCode = it.pcloudCode, displayOrder = it.displayOrder) }
    }
}
