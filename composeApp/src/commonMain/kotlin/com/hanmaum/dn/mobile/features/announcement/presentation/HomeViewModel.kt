package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val banners: List<Announcement> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val error: String? = null,
    val unseenCount: Int = 0
)
class HomeViewModel(
    private val repository: AnnouncementRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    /**
     * Loads (or reloads) announcements. Driven by the screen on every entry so
     * content created in the web app appears without a re-login. Refreshes
     * silently: the spinner shows only on the first load, and a transient
     * refresh failure keeps the currently-shown list instead of replacing it
     * with an error.
     */
    fun loadAnnouncements() {
        viewModelScope.launch {
            notificationRepository.getUnseenCount()
                .onSuccess { count -> _uiState.update { it.copy(unseenCount = count) } }
            // onFailure: keep the previous count; the badge is best-effort.
        }
        viewModelScope.launch {
            val hadData = _uiState.value.run { banners.isNotEmpty() || announcements.isNotEmpty() }
            try {
                _uiState.update { it.copy(isLoading = !hadData, error = null) }

                val fetchedList = repository.getAnnouncements()

                val sortedList = fetchedList.sortedByDescending { it.id }
                val (banners, announcements) = sortedList.partition { it.isPinned }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        banners = banners,
                        announcements = announcements
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = if (hadData) null else e.message) }
            }
        }
    }
}
