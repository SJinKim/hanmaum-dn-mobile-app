package com.hanmaum.dn.mobile.features.album.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.album.domain.model.Album
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumMeta
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumSummary
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumCacheRepository
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumDetailRepository
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlbumsViewModel(
    private val albumsRepository: AlbumsRepository,
    private val albumDetailRepository: AlbumDetailRepository,
    private val cacheRepository: AlbumCacheRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Loading)
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    /**
     * Loads (or reloads) the album list. Driven by the screen on every entry so
     * albums created in the web app appear without a re-login. Shows cached data
     * immediately and only surfaces an error when there is nothing cached to show.
     */
    fun load() {
        viewModelScope.launch {
            val cached = cacheRepository.getCachedAlbumList()
            if (cached != null) {
                _uiState.value = AlbumsUiState.Success(
                    cached.map { album ->
                        val meta = cacheRepository.getCachedMeta(album.pcloudCode)
                        AlbumSummary(album = album, coverUrl = meta?.coverUrl, photoCount = meta?.photoCount)
                    }
                )
            }

            albumsRepository.getAlbums().fold(
                onSuccess = { albums ->
                    cacheRepository.saveAlbumList(albums)
                    val current = (_uiState.value as? AlbumsUiState.Success)?.albums ?: emptyList()
                    val merged = albums.map { album ->
                        val existing = current.find { it.album.publicId == album.publicId }
                        existing?.copy(album = album) ?: run {
                            val meta = cacheRepository.getCachedMeta(album.pcloudCode)
                            AlbumSummary(album = album, coverUrl = meta?.coverUrl, photoCount = meta?.photoCount)
                        }
                    }
                    _uiState.value = AlbumsUiState.Success(merged)
                    merged.forEach { summary -> viewModelScope.launch { resolveAlbumMeta(summary.album) } }
                },
                onFailure = { err ->
                    _uiState.update { current ->
                        if (current !is AlbumsUiState.Success)
                            AlbumsUiState.Error(err.message ?: "앨범 목록 로딩 실패")
                        else current
                    }
                },
            )
        }
    }

    private suspend fun resolveAlbumMeta(album: Album) {
        albumDetailRepository.getFolderContents(album.pcloudCode).onSuccess { items ->
            if (items.isEmpty()) return@onSuccess
            val photoCount = items.size
            albumDetailRepository.getDownloadUrl(album.pcloudCode, items.first().fileId).onSuccess { coverUrl ->
                val newMeta = AlbumMeta(coverUrl = coverUrl, photoCount = photoCount)
                if (cacheRepository.getCachedMeta(album.pcloudCode) != newMeta) {
                    cacheRepository.saveMeta(album.pcloudCode, newMeta)
                    _uiState.update { s ->
                        (s as? AlbumsUiState.Success)?.copy(
                            albums = s.albums.map { summary ->
                                if (summary.album.publicId == album.publicId)
                                    summary.copy(coverUrl = coverUrl, photoCount = photoCount)
                                else summary
                            }
                        ) ?: s
                    }
                }
            }
        }
    }
}
