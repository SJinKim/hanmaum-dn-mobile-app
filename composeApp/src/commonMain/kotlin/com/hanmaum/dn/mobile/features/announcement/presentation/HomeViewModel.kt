package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val banners: List<Announcement> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    /** first name of the signed-in member, for the greeting header */
    val memberName: String? = null,
    /** drives the bell badge; 0 means no badge at all */
    val unseenNotifications: Int = 0,
    val error: String? = null
)
class HomeViewModel(
    private val repository: AnnouncementRepository,
    private val memberRepository: MemberRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadAnnouncements()
        loadMember()
        loadUnseenCount()
    }

    /** Badge count only — a failure here must not blank out the whole header. */
    fun loadUnseenCount() {
        viewModelScope.launch {
            notificationRepository.getUnseenCount()
                .onSuccess { count -> _uiState.update { it.copy(unseenNotifications = count) } }
        }
    }

    /** Greeting header only — reuses the profile endpoint, no new data flow. */
    private fun loadMember() {
        viewModelScope.launch {
            memberRepository.getMyProfile()
                .onSuccess { member ->
                    _uiState.update { it.copy(memberName = "${member.lastName}${member.firstName}") }
                }
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

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
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
