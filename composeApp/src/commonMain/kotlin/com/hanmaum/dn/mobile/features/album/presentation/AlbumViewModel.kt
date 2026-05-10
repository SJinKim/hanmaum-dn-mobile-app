package com.hanmaum.dn.mobile.features.album.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlbumViewModel(private val repository: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            repository.getFolderContents().fold(
                onSuccess = { items ->
                    _uiState.value = AlbumUiState.Success(items = items)
                    // Resolve each photo's CDN URL concurrently; update state as each arrives
                    items.forEach { item ->
                        launch {
                            repository.getDownloadUrl(item.fileId).onSuccess { url ->
                                _uiState.update { s ->
                                    (s as? AlbumUiState.Success)?.copy(
                                        resolvedUrls = s.resolvedUrls + (item.fileId to url)
                                    ) ?: s
                                }
                            }
                        }
                    }
                },
                onFailure = { _uiState.value = AlbumUiState.Error(it.message ?: "앨범 로딩 실패") },
            )
        }
    }
}
