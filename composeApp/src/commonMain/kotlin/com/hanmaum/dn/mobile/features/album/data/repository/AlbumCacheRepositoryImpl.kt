package com.hanmaum.dn.mobile.features.album.data.repository

import com.hanmaum.dn.mobile.features.album.domain.model.Album
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumMeta
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumCacheRepository
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AlbumCacheRepositoryImpl(private val settings: Settings) : AlbumCacheRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getCachedAlbumList(): List<Album>? = runCatching {
        settings.getStringOrNull(KEY_ALBUM_LIST)?.let { json.decodeFromString(it) }
    }.getOrNull()

    override fun saveAlbumList(albums: List<Album>) {
        settings.putString(KEY_ALBUM_LIST, json.encodeToString(albums))
    }

    override fun getCachedMeta(pcloudCode: String): AlbumMeta? = runCatching {
        settings.getStringOrNull(metaKey(pcloudCode))?.let { json.decodeFromString(it) }
    }.getOrNull()

    override fun saveMeta(pcloudCode: String, meta: AlbumMeta) {
        settings.putString(metaKey(pcloudCode), json.encodeToString(meta))
    }

    private fun metaKey(pcloudCode: String) = "album_meta_$pcloudCode"

    companion object {
        private const val KEY_ALBUM_LIST = "albums_list"
    }
}
