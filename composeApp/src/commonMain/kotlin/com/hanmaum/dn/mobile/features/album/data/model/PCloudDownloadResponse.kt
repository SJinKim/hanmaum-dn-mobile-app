package com.hanmaum.dn.mobile.features.album.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PCloudDownloadResponse(
    val result: Int,
    val hosts: List<String> = emptyList(),
    val path: String = "",
)
