package com.hanmaum.dn.mobile.features.events

import com.hanmaum.dn.mobile.features.events.domain.model.CheckInResult
import com.hanmaum.dn.mobile.features.events.domain.model.EventRsvp
import com.hanmaum.dn.mobile.features.events.presentation.EventRsvpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventRsvpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeEventRsvpRepository
    private lateinit var prefs: FakeEventRsvpPreferences

    private fun event(id: String, ann: String? = null) =
        EventRsvp(id, "행사 $id", "2026-07-12T09:00:00+09:00", "2026-07-12T12:00:00+09:00", ann)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeEventRsvpRepository()
        prefs = FakeEventRsvpPreferences()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = EventRsvpViewModel(repo, prefs)

    @Test
    fun refresh_filtersHandledEventsAndShowsSheet() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1"), event("e2")))
        prefs.markHandled("e1")

        val viewModel = vm()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("e2"), state.events.map { it.publicId })
        assertTrue(state.visible)
    }

    @Test
    fun refresh_hidesSheetWhenNothingPending() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        prefs.markHandled("e1")

        val viewModel = vm()
        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.visible)
    }

    @Test
    fun checkIn_success_persistsAndMarksRowCheckedIn() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        repo.checkInResults["e1"] = CheckInResult.Success(
            com.hanmaum.dn.mobile.features.events.domain.model.EventRsvpCheckIn("e1", "행사 e1", "2026-07-12T10:00:00+09:00"),
        )
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.checkIn("e1"); advanceUntilIdle()

        assertTrue("e1" in viewModel.uiState.value.checkedInIds)
        assertTrue(prefs.isHandled("e1"))
    }

    @Test
    fun checkIn_alreadyRegistered_treatedAsSuccess() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        repo.checkInResults["e1"] = CheckInResult.AlreadyRegistered
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.checkIn("e1"); advanceUntilIdle()

        assertTrue("e1" in viewModel.uiState.value.checkedInIds)
        assertTrue(prefs.isHandled("e1"))
    }

    @Test
    fun checkIn_windowClosed_setsRowErrorAndRefreshes() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        repo.checkInResults["e1"] = CheckInResult.WindowClosed
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        // After the failed attempt, refresh() reloads — simulate the now-closed event dropping off.
        repo.activeResult = Result.success(emptyList())
        viewModel.checkIn("e1"); advanceUntilIdle()

        assertFalse(prefs.isHandled("e1"))
        assertFalse(viewModel.uiState.value.visible) // refresh cleared the empty list
    }

    @Test
    fun checkIn_ignoresSecondTapWhileInFlight() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1")))
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.checkIn("e1") // sets checkingInId synchronously, schedules the coroutine
        viewModel.checkIn("e1") // guard must reject this one
        advanceUntilIdle()

        assertEquals(1, repo.checkInCallCount)
    }

    @Test
    fun dismissAll_marksHandledAndHides() = runTest(testDispatcher) {
        repo.activeResult = Result.success(listOf(event("e1"), event("e2")))
        val viewModel = vm()
        viewModel.refresh(); advanceUntilIdle()

        viewModel.dismissAll()

        assertFalse(viewModel.uiState.value.visible)
        assertTrue(prefs.isHandled("e1"))
        assertTrue(prefs.isHandled("e2"))
    }
}
