package com.hanmaum.dn.mobile.features.album.presentation.albums

import com.hanmaum.dn.mobile.features.album.domain.model.AlbumSummary

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data class Success(val albums: List<AlbumSummary>) : AlbumsUiState
    data class Error(val message: String) : AlbumsUiState
}
