package com.hanmaum.dn.mobile.features.album.domain.repository

import com.hanmaum.dn.mobile.features.album.domain.model.Album
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumMeta

interface AlbumCacheRepository {
    fun getCachedAlbumList(): List<Album>?
    fun saveAlbumList(albums: List<Album>)
    fun getCachedMeta(pcloudCode: String): AlbumMeta?
    fun saveMeta(pcloudCode: String, meta: AlbumMeta)
}
