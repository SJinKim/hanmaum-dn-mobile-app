package com.hanmaum.dn.mobile.features.geofence.domain

import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
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

private class FakeLocationPreferences(
    private var sharing: Boolean = true,
) : LocationPreferences {
    override fun isSharingEnabled() = sharing
    override fun setSharingEnabled(value: Boolean) { sharing = value }
    override fun isPromptDismissed() = true
    override fun setPromptDismissed(value: Boolean) = Unit
    override fun clear() { sharing = false }
}

private class FakeNotificationService : NotificationService {
    var notificationCount = 0
    override suspend fun isNotificationPermissionGranted() = true
    override suspend fun showAttendanceNotification() { notificationCount++ }
}

private class FakeAttendanceRepository(
    private val definitions: List<AttendanceDefinition> = emptyList()
) : AttendanceRepository {
    override suspend fun getActiveDefinitions() = Result.success(definitions)
    override suspend fun checkIn() = Result.success(Unit)
}

private fun todayName(): String =
    kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.name

// ── Tests ─────────────────────────────────────────────────────────────────────

class GeofenceCoordinatorTest {

    @Test
    fun initialize_doesNotRegisterWhenSharingIsOff() = runTest {
        // The OS permission alone must not start background monitoring — the
        // member's own switch is the second gate.
        val fakeGeofence = FakeGeofenceManager(permissionGranted = true)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(),
            FakeLocationPreferences(sharing = false), scope = this
        )

        coordinator.initialize()

        assertFalse(fakeGeofence.isMonitoring, "Monitoring started although sharing is off")
    }

    @Test
    fun disable_stopsMonitoring() = runTest {
        val fakeGeofence = FakeGeofenceManager(permissionGranted = true)
        val coordinator = GeofenceCoordinator(
            FakeChurchLocationRepository(), FakeAttendanceRepository(),
            fakeGeofence, FakeNotificationService(), FakeLocationPreferences(), scope = this
        )
        coordinator.initialize()
        assertTrue(fakeGeofence.isMonitoring)

        coordinator.disable()

        assertFalse(fakeGeofence.isMonitoring, "Switching the setting off left the geofence running")
    }

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
