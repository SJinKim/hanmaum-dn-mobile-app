package com.hanmaum.dn.mobile.features.album.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val pcloudCode: String,
    private val albumName: String,
    private val repository: AlbumDetailRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AlbumDetailUiState.Loading
            repository.getFolderContents(pcloudCode).fold(
                onSuccess = { items ->
                    _uiState.value = AlbumDetailUiState.Success(items = items)
                    items.forEach { item ->
                        launch {
                            repository.getDownloadUrl(pcloudCode, item.fileId).onSuccess { url ->
                                _uiState.update { s ->
                                    (s as? AlbumDetailUiState.Success)?.copy(
                                        resolvedUrls = s.resolvedUrls + (item.fileId to url)
                                    ) ?: s
                                }
                            }
                        }
                    }
                },
                onFailure = { _uiState.value = AlbumDetailUiState.Error(it.message ?: "앨범 로딩 실패") },
            )
        }
    }
}
