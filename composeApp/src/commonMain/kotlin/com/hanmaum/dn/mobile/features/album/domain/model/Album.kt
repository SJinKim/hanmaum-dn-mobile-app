package com.hanmaum.dn.mobile.features.album.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val publicId: String,
    val name: String,
    val pcloudCode: String,
    val displayOrder: Int,
)
