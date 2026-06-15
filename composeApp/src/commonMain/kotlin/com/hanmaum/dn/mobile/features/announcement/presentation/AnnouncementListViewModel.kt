package com.hanmaum.dn.mobile.features.announcement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Time bucket an announcement falls into, relative to the current month. */
enum class AnnouncementSectionKey { THIS_MONTH, LAST_MONTH, EARLIER }

data class AnnouncementSection(
    val key: AnnouncementSectionKey,
    val items: List<Announcement>,
)

data class ListUiState(
    val isLoading: Boolean = false,
    val list: List<Announcement> = emptyList(),
    val sections: List<AnnouncementSection> = emptyList(),
    val error: String? = null
)

class AnnouncementListViewModel(
    private val repository: AnnouncementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = repository.getAnnouncements()
                // Newest first — ISO date strings sort chronologically.
                val sorted = result.sortedByDescending { it.startAt }
                _uiState.update {
                    it.copy(isLoading = false, list = sorted, sections = buildSections(sorted))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun buildSections(sorted: List<Announcement>): List<AnnouncementSection> {
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return bucketByMonth(sorted, currentMonthIndex = now.year * 12 + now.monthNumber)
    }
}

/**
 * Buckets [sorted] announcements into This Month / Last Month / Earlier relative to
 * [currentMonthIndex] (year*12 + month). Pure and clock-independent for testability.
 */
internal fun bucketByMonth(
    sorted: List<Announcement>,
    currentMonthIndex: Int,
): List<AnnouncementSection> {
    val grouped = LinkedHashMap<AnnouncementSectionKey, MutableList<Announcement>>()
    for (announcement in sorted) {
        val idx = monthIndexOf(announcement.startAt)
        val key = when {
            idx == null -> AnnouncementSectionKey.EARLIER
            idx >= currentMonthIndex -> AnnouncementSectionKey.THIS_MONTH
            idx == currentMonthIndex - 1 -> AnnouncementSectionKey.LAST_MONTH
            else -> AnnouncementSectionKey.EARLIER
        }
        grouped.getOrPut(key) { mutableListOf() }.add(announcement)
    }

    // Fixed display order, skipping empty buckets.
    return listOf(
        AnnouncementSectionKey.THIS_MONTH,
        AnnouncementSectionKey.LAST_MONTH,
        AnnouncementSectionKey.EARLIER,
    ).mapNotNull { key -> grouped[key]?.let { AnnouncementSection(key, it) } }
}

/** year*12 + month from the leading `YYYY-MM-DD` of [startAt], or null if unparseable. */
internal fun monthIndexOf(startAt: String): Int? = try {
    val date = LocalDate.parse(startAt.take(10))
    date.year * 12 + date.monthNumber
} catch (e: Exception) {
    null
}
