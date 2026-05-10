package com.hanmaum.dn.mobile.features.attendance

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

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAttendanceRepository()
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

    private fun todayDefinition() = AttendanceDefinition(
        publicId = "def-1",
        title = "Sunday Service",
        dayOfWeek = todayName(),
        windowStart = "00:00:00",
        windowEnd = "23:59:59",
    )

    @Test
    fun initial_state_has_no_definition_and_is_not_checked_in() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(emptyList())

        val viewModel = AttendanceViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.definition)
        assertFalse(state.isCheckedIn)
    }

    @Test
    fun checkIn_success_sets_isCheckedIn_true() = runTest(testDispatcher) {
        fakeRepo.definitionsResult = Result.success(listOf(todayDefinition()))
        fakeRepo.checkInResult = Result.success(Unit)

        val viewModel = AttendanceViewModel(fakeRepo)
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

        val viewModel = AttendanceViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.checkIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCheckedIn)
        val checkInError = state.checkInError
        assertNotNull(checkInError)
        assertTrue(checkInError.isNotEmpty())
    }
}
