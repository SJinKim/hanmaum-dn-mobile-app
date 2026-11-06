package com.hanmaum.dn.mobile.features.album.data.repository

import com.hanmaum.dn.mobile.core.network.legacyOrWrappedBody
import com.hanmaum.dn.mobile.features.album.data.model.AlbumDto
import com.hanmaum.dn.mobile.features.album.domain.model.Album
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException

class AlbumsRepositoryImpl(private val client: HttpClient) : AlbumsRepository {
    override suspend fun getAlbums(): Result<List<Album>> = try {
        Result.success(client.get("albums")
            .legacyOrWrappedBody<List<AlbumDto>>()
            .sortedBy { it.displayOrder }
            .map { Album(publicId = it.publicId, name = it.name, pcloudCode = it.pcloudCode, displayOrder = it.displayOrder) })
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
