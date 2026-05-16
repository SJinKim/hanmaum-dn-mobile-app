package com.hanmaum.dn.mobile.features.album.domain.repository

import com.hanmaum.dn.mobile.features.album.domain.model.Album

interface AlbumsRepository {
    suspend fun getAlbums(): Result<List<Album>>
}
