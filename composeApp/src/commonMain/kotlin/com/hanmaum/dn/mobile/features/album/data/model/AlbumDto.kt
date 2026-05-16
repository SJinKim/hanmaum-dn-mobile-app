package com.hanmaum.dn.mobile.features.album.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val publicId: String,
    val name: String,
    val pcloudCode: String,
    val displayOrder: Int,
)
