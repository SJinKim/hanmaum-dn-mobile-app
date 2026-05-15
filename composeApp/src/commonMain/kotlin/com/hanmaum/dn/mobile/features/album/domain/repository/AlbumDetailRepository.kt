package com.hanmaum.dn.mobile.features.album.domain.repository

import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem

interface AlbumDetailRepository {
    suspend fun getFolderContents(pcloudCode: String): Result<List<AlbumItem>>
    suspend fun getDownloadUrl(pcloudCode: String, fileId: Long): Result<String>
}
