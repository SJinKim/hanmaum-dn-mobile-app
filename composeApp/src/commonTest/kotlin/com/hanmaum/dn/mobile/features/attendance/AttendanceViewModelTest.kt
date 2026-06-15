package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeAttendanceRepository
    private lateinit var fakePrefs: FakeAttendancePreferences

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAttendanceRepository()
        fakePrefs = FakeAttendancePreferences()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Returns today's day-of-week name (e.g. "SUNDAY") so the VM's load() picks it up. */
    private fun todayName(): String =
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .dayOfWeek.name

    /** Returns today's ISO date string, matching how the VM stamps a check-in. */
    private fun todayIso(): String =
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date.toString()

    private fun todayDefinition() = AttendanceDefinition(
        publicId = "def-1",
        title = "Sunday Service",
        dayOfWeek = todayName(),
        windowStart = "00:00:00",
        windowEnd = "23:59:59",
    )

    private fun newViewModel() = AttendanceViewModel(fakeRepo, fakePrefs)

    @Test
    fun initial_state_has_no_definition_and_is_not_checked_in() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(emptyList())

        val viewModel = newViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.definition)
        assertFalse(state.isCheckedIn)
    }

    @Test
    fun checkIn_success_sets_isCheckedIn_true() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(listOf(todayDefinition()))
        fakeRepo.checkInResult = Result.success(
            AttendanceCheckIn(definitionPublicId = "def-1", definitionTitle = "Sunday Service", attendanceDate = todayIso()),
        )

        val viewModel = newViewModel()
        advanceUntilIdle()

        // Sanity: definition should be loaded for today
        assertNotNull(viewModel.uiState.value.definition)

        viewModel.checkIn()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCheckedIn)
        assertNull(viewModel.uiState.value.checkInError)
    }

    @Test
    fun checkIn_failure_shows_error_message() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(listOf(todayDefinition()))
        fakeRepo.checkInResult = Result.failure(RuntimeException("network error"))

        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.checkIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCheckedIn)
        val checkInError = state.checkInError
        assertNotNull(checkInError)
        assertTrue(checkInError.isNotEmpty())
    }

    @Test
    fun checkIn_success_persists_to_preferences() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(listOf(todayDefinition()))
        fakeRepo.checkInResult = Result.success(
            AttendanceCheckIn(definitionPublicId = "def-1", definitionTitle = "Sunday Service", attendanceDate = todayIso()),
        )

        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.checkIn()
        advanceUntilIdle()

        // The successful check-in must be persisted so a relaunch restores the checked-in state.
        assertTrue(fakePrefs.isCheckedIn("def-1", todayIso()))
    }

    @Test
    fun load_restores_checkedIn_when_already_checked_in_today() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(listOf(todayDefinition()))
        // Simulate a prior session: the check-in for today's definition is already persisted.
        fakePrefs.markCheckedIn("def-1", todayIso())

        val viewModel = newViewModel()
        advanceUntilIdle()

        // Without ever pressing the button, the screen must show the already-checked-in state.
        assertTrue(viewModel.uiState.value.isCheckedIn)
    }

    @Test
    fun load_does_not_restore_checkedIn_for_a_different_day() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(listOf(todayDefinition()))
        // A check-in from a previous date must not carry over to today.
        fakePrefs.markCheckedIn("def-1", "2000-01-01")

        val viewModel = newViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCheckedIn)
    }
}
