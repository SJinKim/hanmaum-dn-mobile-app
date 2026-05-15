package com.hanmaum.dn.mobile.features.album.presentation

import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem

sealed interface AlbumDetailUiState {
    data object Loading : AlbumDetailUiState
    data class Success(
        val items: List<AlbumItem>,
        val resolvedUrls: Map<Long, String> = emptyMap(),
    ) : AlbumDetailUiState
    data class Error(val message: String) : AlbumDetailUiState
}
