package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.model.AnnouncementLookup
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val announcement: Announcement? = null,
    // Terminal, non-retryable: the announcement is no longer in the active feed.
    val gone: Boolean = false,
    // Retryable transient failure (network / non-2xx). Message is rendered from i18n.
    val hasError: Boolean = false,
)

class AnnouncementDetailViewModel(
    private val announcementId: String,
    private val repository: AnnouncementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAnnouncement()
    }

    fun loadAnnouncement() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, gone = false, hasError = false) }
            when (val result = repository.getAnnouncementById(announcementId)) {
                is AnnouncementLookup.Found ->
                    _uiState.update { it.copy(isLoading = false, announcement = result.announcement) }
                AnnouncementLookup.NotFound ->
                    _uiState.update { it.copy(isLoading = false, gone = true) }
                AnnouncementLookup.Error ->
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }
}