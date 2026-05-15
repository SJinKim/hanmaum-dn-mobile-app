package com.hanmaum.dn.mobile.features.album.domain.model

data class AlbumSummary(
    val album: Album,
    val coverUrl: String? = null,
    val photoCount: Int? = null,
)
