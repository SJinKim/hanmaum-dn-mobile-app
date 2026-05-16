package com.hanmaum.dn.mobile.features.album.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AlbumMeta(val coverUrl: String, val photoCount: Int)
