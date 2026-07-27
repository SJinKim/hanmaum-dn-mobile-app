package com.hanmaum.dn.mobile.features.announcement.presentation

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.model.AnnouncementLookup
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class DetailFakeAnnouncementRepository(
    private val lookup: AnnouncementLookup,
) : AnnouncementRepository {
    override suspend fun getAnnouncements(): List<Announcement> = emptyList()
    override suspend fun getAnnouncementById(id: String): AnnouncementLookup = lookup
}

private val detailSample = Announcement(
    id = "a1", title = "t", body = "b",
    startAt = "2026-07-01", endAt = null, isPinned = false, category = "NOTICE",
)

class AnnouncementDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `found announcement lands in state`() = runTest(dispatcher) {
        val vm = AnnouncementDetailViewModel("a1", DetailFakeAnnouncementRepository(AnnouncementLookup.Found(detailSample)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(detailSample, state.announcement)
        assertFalse(state.gone)
        assertFalse(state.hasError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `not found sets gone without error`() = runTest(dispatcher) {
        val vm = AnnouncementDetailViewModel("a1", DetailFakeAnnouncementRepository(AnnouncementLookup.NotFound))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.gone)
        assertFalse(state.hasError)
        assertNull(state.announcement)
    }

    @Test
    fun `error sets hasError and stays retryable`() = runTest(dispatcher) {
        val vm = AnnouncementDetailViewModel("a1", DetailFakeAnnouncementRepository(AnnouncementLookup.Error))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.hasError)
        assertFalse(state.gone)
    }
}
