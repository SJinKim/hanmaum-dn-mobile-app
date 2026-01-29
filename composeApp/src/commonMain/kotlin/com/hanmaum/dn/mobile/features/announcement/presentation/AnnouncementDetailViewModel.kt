package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val announcement: Announcement? = null,
    val error: String? = null
)

class AnnouncementDetailViewModel(
    private val token: String,
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = repository.getAnnouncementById(token, announcementId)
                if (result != null) {
                    _uiState.update { it.copy(isLoading = false, announcement = result) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Eintrag nicht gefunden") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}