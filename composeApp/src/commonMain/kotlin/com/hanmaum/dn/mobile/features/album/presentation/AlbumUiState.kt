package com.hanmaum.dn.mobile.features.album.presentation

import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem

sealed interface AlbumUiState {
    data object Loading : AlbumUiState
    data class Success(
        val items: List<AlbumItem>,
        val resolvedUrls: Map<Long, String> = emptyMap(),
    ) : AlbumUiState
    data class Error(val message: String) : AlbumUiState
}
