package com.hanmaum.dn.mobile.features.album.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PCloudFolderResponse(
    val result: Int,
    val metadata: PCloudFolderMetadata? = null,
)

@Serializable
data class PCloudFolderMetadata(
    val name: String = "",
    val isfolder: Boolean = false,
    val contents: List<PCloudFileEntry> = emptyList(),
)

@Serializable
data class PCloudFileEntry(
    val name: String,
    val isfolder: Boolean,
    val fileid: Long? = null,
    val size: Long? = null,
)
