package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.core.push.PushEventBus
import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.features.announcement.data.repository.AnnouncementRepositoryImpl
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerse
import com.hanmaum.dn.mobile.features.verse.domain.model.WeeklyVerse
import com.hanmaum.dn.mobile.features.verse.domain.repository.VerseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val banners: List<Announcement> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val error: String? = null,
    val unseenCount: Int = 0,
    /** Greeting name in the header; null until the profile call returns. */
    val memberName: String? = null,
    /**
     * Today's passage, or null when there is none to show — no plan entry for
     * the day (Sundays) or the call failed. Both hide the card; the passage is
     * a nice-to-have and must never turn Home into an error screen.
     */
    val dailyVerse: DailyVerse? = null,
    /**
     * This week's memory verse, or null when none is set for the running week
     * or the call failed. Hides the card, same reasoning as [dailyVerse].
     */
    val weeklyVerse: WeeklyVerse? = null,
)
class HomeViewModel(
    private val repository: AnnouncementRepository,
    private val notificationRepository: NotificationRepository,
    private val memberRepository: MemberRepository,
    private val verseRepository: VerseRepository,
    private val pushManager: PushManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var tokenRegistered = false

    init {
        viewModelScope.launch {
            PushEventBus.tokenRefreshes.collect { token ->
                notificationRepository.registerDeviceToken(token, pushManager.platform)
            }
        }
    }

    private fun registerTokenIfNeeded() {
        if (tokenRegistered) return
        viewModelScope.launch {
            pushManager.currentToken()?.let { token ->
                notificationRepository.registerDeviceToken(token, pushManager.platform)
                    .onSuccess { tokenRegistered = true }
            }
        }
    }

    /**
     * Loads (or reloads) announcements. Driven by the screen on every entry so
     * content created in the web app appears without a re-login. Refreshes
     * silently: the spinner shows only on the first load, and a transient
     * refresh failure keeps the currently-shown list instead of replacing it
     * with an error.
     */
    /**
     * Re-reads the bell badge on its own.
     *
     * The count is cleared server-side the moment the notification list opens,
     * so Home has to re-read it on the way back or it keeps showing a stale
     * number. Separate from [loadAnnouncements] because the screen calls it on
     * every resume, and refetching the whole list for a badge would be waste.
     */
    fun loadUnseenCount() {
        viewModelScope.launch {
            notificationRepository.getUnseenCount()
                .onSuccess { count -> _uiState.update { it.copy(unseenCount = count) } }
            // onFailure: keep the previous count; the badge is best-effort.
        }
    }

    private fun loadMember() {
        viewModelScope.launch {
            memberRepository.getMyProfile()
                .onSuccess { member -> _uiState.update { it.copy(memberName = "${member.lastName} ${member.firstName}") } }
            // onFailure: the header simply greets without a name.
        }
    }

    /**
     * Reads both verse cards. Failure is silent on purpose: a card simply stays
     * hidden, exactly as it does when there is nothing planned for it.
     *
     * Two calls rather than one, launched together — they are independent, and
     * a missing weekly verse must not keep the daily passage off the screen.
     */
    private fun loadVerses() {
        viewModelScope.launch {
            verseRepository.getTodayVerse()
                .onSuccess { verse -> _uiState.update { it.copy(dailyVerse = verse) } }
        }
        viewModelScope.launch {
            verseRepository.getWeeklyVerse()
                .onSuccess { verse -> _uiState.update { it.copy(weeklyVerse = verse) } }
        }
    }

    fun loadAnnouncements() {
        registerTokenIfNeeded()
        loadUnseenCount()
        loadMember()
        loadVerses()
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
