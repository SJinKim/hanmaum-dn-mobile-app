package com.hanmaum.dn.mobile.features.geofence.domain

import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceCheckIn
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceHistory
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ── Fakes ────────────────────────────────────────────────────────────────────

private class FakeChurchLocationRepository(
    private val result: Result<ChurchLocation> =
        Result.success(ChurchLocation(37.0, 127.0, 100.0))
) : ChurchLocationRepository {
    override suspend fun getChurchLocation() = result
}

private class FakeGeofenceManager(
    private val permissionGranted: Boolean = true
) : GeofenceManager {
    var capturedOnEnter: (() -> Unit)? = null
    var isMonitoring = false

    override fun isLocationPermissionGranted() = permissionGranted
    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) {
        capturedOnEnter = onEnter
        isMonitoring = true
    }
    override fun stopMonitoring() { isMonitoring = false }
}

private class FakeNotificationService : NotificationService {
    var notificationCount = 0
    override fun isNotificationPermissionGranted() = true
    override fun showAttendanceNotification() { notificationCount++ }
}

private class FakeAttendanceRepository(
    private val definitions: List<AttendanceDefinition> = emptyList()
) : AttendanceRepository {
    override suspend fun getActiveDefinitions() = Result.success(definitions)
    override suspend fun checkIn() = Result.success(
        AttendanceCheckIn(definitionPublicId = "def", definitionTitle = "Service", attendanceDate = "2026-06-15"),
    )
    // The coordinator never reads these; they exist to satisfy the interface.
    override suspend fun getMySummary() = Result.success(
        AttendanceSummary(monthAttended = 0, monthTotal = 0, yearAttended = 0, yearToDateTotal = 0, rate = 0.0),
    )
    override suspend fun getMyHistory() = Result.success(
        AttendanceHistory(from = "2026-06-06", to = "2026-09-04", entries = emptyList()),
    )
}

private class FakeLocationPreferences(
    private var sharingEnabled: Boolean = true,
) : LocationPreferences {
    override fun isSharingEnabled() = sharingEnabled
    override fun setSharingEnabled(value: Boolean) { sharingEnabled = value }
    override fun isPromptDismissed() = false
    override fun setPromptDismissed(value: Boolean) {}
}

private fun todayName(): String =
    kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.name

// ── Tests ─────────────────────────────────────────────────────────────────────

class GeofenceCoordinatorTest {

    @Test
    fun initialize_registersGeofenceWhenPermissionGranted() = runTest {
        val fakeGeofence = FakeGeofenceManager(permissionGranted = true)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(), FakeLocationPreferences(), scope = this
        )

        coordinator.initialize()

        assertTrue(fakeGeofence.isMonitoring)
    }

    @Test
    fun initialize_doesNotRegisterWhenPermissionDenied() = runTest {
        val fakeGeofence = FakeGeofenceManager(permissionGranted = false)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(), FakeLocationPreferences(), scope = this
        )

        coordinator.initialize()

        assertFalse(fakeGeofence.isMonitoring)
    }

    @Test
    fun initialize_doesNotRegisterWhenLocationFetchFails() = runTest {
        val fakeGeofence = FakeGeofenceManager()
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(Result.failure(Exception("network error"))),
            FakeAttendanceRepository(), fakeGeofence, FakeNotificationService(), FakeLocationPreferences(), scope = this
        )

        coordinator.initialize() // must not throw

        assertFalse(fakeGeofence.isMonitoring)
    }

    @Test
    fun initialize_isNoOpWhenCalledTwice() = runTest {
        var startCallCount = 0
        val countingGeofence = object : GeofenceManager {
            override fun isLocationPermissionGranted() = true
            override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) { startCallCount++ }
            override fun stopMonitoring() {}
        }
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            countingGeofence, FakeNotificationService(), FakeLocationPreferences(), scope = this
        )

        coordinator.initialize()
        coordinator.initialize()

        assertEquals(1, startCallCount)
    }

    @Test
    fun notifyEntry_firesNotificationWhenInsideActiveWindow() = runTest {
        val fakeNotification = FakeNotificationService()
        val fakeGeofence = FakeGeofenceManager()
        val definitions = listOf(
            AttendanceDefinition("1", "Morning Service", todayName(), "00:00:00", "23:59:59")
        )
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(definitions),
            fakeGeofence, fakeNotification, FakeLocationPreferences(), scope = this
        )

        coordinator.initialize()
        coordinator.notifyEntry()
        advanceUntilIdle()

        assertEquals(1, fakeNotification.notificationCount)
    }

    @Test
    fun notifyEntry_doesNotFireNotificationOutsideWindow() = runTest {
        val fakeNotification = FakeNotificationService()
        val fakeGeofence = FakeGeofenceManager()
        // Compute a window 3 hours behind current time (mod 24) — always in the past regardless
        // of what time the test runs, including at midnight. The isCurrentlyInWindow check does
        // not wrap around midnight, so a window at hour H with current time at H+3 is never current.
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val pastHour = (now.hour - 3 + 24) % 24
        val windowStart = pastHour.toString().padStart(2, '0') + ":00:00"
        val windowEnd   = pastHour.toString().padStart(2, '0') + ":00:01"
        val definitions = listOf(
            AttendanceDefinition("1", "Morning Service", todayName(), windowStart, windowEnd)
        )
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(definitions),
            fakeGeofence, fakeNotification, FakeLocationPreferences(), scope = this
        )

        coordinator.initialize()
        coordinator.notifyEntry()
        advanceUntilIdle()

        assertEquals(0, fakeNotification.notificationCount)
    }

    @Test
    fun initialize_doesNotRegisterWhenSharingDisabled() = runTest {
        val fakeGeofence = FakeGeofenceManager(permissionGranted = true)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(),
            FakeLocationPreferences(sharingEnabled = false), scope = this
        )

        coordinator.initialize()

        assertFalse(fakeGeofence.isMonitoring)
    }

    @Test
    fun stop_stopsMonitoringAndAllowsReinitialize() = runTest {
        val fakeGeofence = FakeGeofenceManager(permissionGranted = true)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(),
            FakeLocationPreferences(), scope = this
        )

        coordinator.initialize()
        assertTrue(fakeGeofence.isMonitoring)

        coordinator.stop()
        assertFalse(fakeGeofence.isMonitoring)

        coordinator.initialize()
        assertTrue(fakeGeofence.isMonitoring)
    }

    @Test
    fun notifyEntry_doesNotFireWhenNoServiceToday() = runTest {
        val fakeNotification = FakeNotificationService()
        val fakeGeofence = FakeGeofenceManager()
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(emptyList()),
            fakeGeofence, fakeNotification, FakeLocationPreferences(), scope = this
        )

        coordinator.initialize()
        coordinator.notifyEntry()
        advanceUntilIdle()

        assertEquals(0, fakeNotification.notificationCount)
    }
}
