package com.hanmaum.dn.mobile.features.album.domain.repository

import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem

interface AlbumRepository {
    suspend fun getFolderContents(): Result<List<AlbumItem>>
    suspend fun getDownloadUrl(fileId: Long): Result<String>
}
