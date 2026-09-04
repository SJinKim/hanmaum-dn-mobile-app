package com.hanmaum.dn.mobile.features.attendance.presentation.history

import com.hanmaum.dn.mobile.features.attendance.FakeAttendanceRepository
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceEntry
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceHistory
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeAttendanceRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeAttendanceRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun entry(date: String, title: String, at: String?, checkedIn: Boolean = true) =
        AttendanceEntry(
            definitionPublicId = title,
            definitionTitle = title,
            date = date,
            checkedIn = checkedIn,
            checkedInAt = at,
        )

    private fun withHistory(vararg entries: AttendanceEntry): AttendanceHistoryViewModel {
        repo.historyResult = Result.success(
            AttendanceHistory(from = "2025-08-01", to = "2026-09-05", entries = entries.toList()),
        )
        return AttendanceHistoryViewModel(repo)
    }

    /** Puts the view model on a known month regardless of today's date. */
    private fun AttendanceHistoryViewModel.goTo(year: Int, month: Int, day: Int?) {
        val cur = uiState.value
        var steps = (cur.year * 12 + cur.month) - (year * 12 + month)
        while (steps > 0) { previousMonth(); steps-- }
        while (steps < 0) { nextMonth(); steps++ }
        day?.let { selectDay(it) }
    }

    @Test
    fun onlyAttendedOccurrencesReachTheCalendar() = runTest {
        // A missed occurrence must not put a mark on the calendar — this screen
        // answers "when was I there", not "what did I miss".
        val vm = withHistory(
            entry("2026-08-16", "주일예배", "2026-08-16T09:12:00Z"),
            entry("2026-08-23", "주일예배", null, checkedIn = false),
        )
        advanceUntilIdle()
        vm.goTo(2026, 8, 16)

        assertEquals(1, vm.uiState.value.attended.size)
        assertEquals(setOf(16), vm.uiState.value.markedDays)
    }

    @Test
    fun marksCoverOnlyTheShownMonth() = runTest {
        val vm = withHistory(
            entry("2026-08-16", "주일예배", "2026-08-16T09:12:00Z"),
            entry("2026-07-19", "주일예배", "2026-07-19T09:05:00Z"),
        )
        advanceUntilIdle()

        vm.goTo(2026, 8, null)
        assertEquals(setOf(16), vm.uiState.value.markedDays)
        vm.previousMonth()
        assertEquals(setOf(19), vm.uiState.value.markedDays)
    }

    @Test
    fun severalServicesOnOneDayAreAllListedChronologically() = runTest {
        val vm = withHistory(
            entry("2026-08-16", "저녁 기도회", "2026-08-16T19:31:00Z"),
            entry("2026-08-16", "주일예배", "2026-08-16T09:12:00Z"),
        )
        advanceUntilIdle()
        vm.goTo(2026, 8, 16)

        val day = vm.uiState.value.entriesForSelectedDay
        assertEquals(2, day.size)
        assertEquals("주일예배", day[0].definitionTitle, "earlier check-in comes first")
        assertEquals("저녁 기도회", day[1].definitionTitle)
    }

    @Test
    fun aDayWithoutAttendanceListsNothing() = runTest {
        val vm = withHistory(entry("2026-08-16", "주일예배", "2026-08-16T09:12:00Z"))
        advanceUntilIdle()
        vm.goTo(2026, 8, 18)

        assertTrue(vm.uiState.value.entriesForSelectedDay.isEmpty())
        assertFalse(vm.uiState.value.attended.isEmpty(), "but the record itself is not empty")
    }

    @Test
    fun anEmptyRecordLoadsWithoutFailing() = runTest {
        val vm = withHistory()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.attended.isEmpty())
        assertFalse(vm.uiState.value.failed, "empty is not the same as broken")
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun aFailedLoadIsMarkedFailedNotEmpty() = runTest {
        repo.historyResult = Result.failure(IllegalStateException("offline"))
        val vm = AttendanceHistoryViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.failed)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.attended.isEmpty())
    }

    @Test
    fun changingMonthClearsTheSelectedDay() = runTest {
        // Keeping day 31 while moving to February would select a day that does
        // not exist in that month.
        val vm = withHistory(entry("2026-08-16", "주일예배", "2026-08-16T09:12:00Z"))
        advanceUntilIdle()
        vm.goTo(2026, 8, 31)
        assertEquals(31, vm.uiState.value.selectedDay)

        vm.nextMonth()
        assertNull(vm.uiState.value.selectedDay)
    }

    @Test
    fun monthNavigationRollsOverTheYear() = runTest {
        val vm = withHistory()
        advanceUntilIdle()
        vm.goTo(2026, 12, null)

        vm.nextMonth()
        assertEquals(1, vm.uiState.value.month)
        assertEquals(2027, vm.uiState.value.year)

        vm.previousMonth()
        assertEquals(12, vm.uiState.value.month)
        assertEquals(2026, vm.uiState.value.year)
    }

    @Test
    fun retryClearsTheFailureAndLoadsAgain() = runTest {
        repo.historyResult = Result.failure(IllegalStateException("offline"))
        val vm = AttendanceHistoryViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.failed)

        repo.historyResult = Result.success(
            AttendanceHistory("2025-08-01", "2026-09-05", listOf(entry("2026-08-16", "주일예배", "2026-08-16T09:12:00Z"))),
        )
        vm.load()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.failed)
        assertEquals(1, vm.uiState.value.attended.size)
    }
}
