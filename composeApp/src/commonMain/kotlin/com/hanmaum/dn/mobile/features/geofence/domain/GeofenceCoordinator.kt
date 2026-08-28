package com.hanmaum.dn.mobile.features.geofence.domain

import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceDefinition
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GeofenceCoordinator(
    private val churchLocationRepository: ChurchLocationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val geofenceManager: GeofenceManager,
    private val notificationService: NotificationService,
    private val locationPreferences: LocationPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private val initMutex = Mutex()
    private var isRegistered = false

    /**
     * Fetches the church location from the server and registers the OS geofence.
     *
     * Two gates, not one: the OS must have granted location access, and the
     * member must have switched location sharing on in Settings. Either one
     * missing means no monitoring — background location is not something to
     * start on a permission alone.
     *
     * Call after login is confirmed (SplashViewModel when status == ACTIVE) and
     * whenever the member turns the setting on.
     */
    suspend fun initialize() {
        if (isRegistered) return // fast path — avoids lock on steady state
        initMutex.withLock {
            if (isRegistered) return@withLock // double-check inside lock
            if (!locationPreferences.isSharingEnabled()) return@withLock
            if (!geofenceManager.isLocationPermissionGranted()) return@withLock

            val location = churchLocationRepository.getChurchLocation().getOrElse { error ->
                println("[GeofenceCoordinator] Failed to fetch church location: ${error.message}")
                return@withLock
            }

            geofenceManager.startMonitoring(location) { notifyEntry() }
            isRegistered = true
        }
    }

    /**
     * Called by the platform geofence implementation (BroadcastReceiver on Android,
     * CLLocationManager delegate on iOS) when the user enters the monitored region.
     */
    /** Turning the setting off must actually stop the OS monitoring. */
    fun disable() {
        geofenceManager.stopMonitoring()
        isRegistered = false
    }

    fun notifyEntry() {
        scope.launch { onGeofenceEntered() }
    }

    private suspend fun onGeofenceEntered() {
        val definitions = attendanceRepository.getActiveDefinitions().getOrElse { return }
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayName = now.dayOfWeek.name
        val todayDef = definitions.firstOrNull { it.dayOfWeek == todayName } ?: return
        if (isCurrentlyInWindow(todayDef, now.hour, now.minute, now.second)) {
            notificationService.showAttendanceNotification()
        }
    }

    private fun isCurrentlyInWindow(def: AttendanceDefinition, hour: Int, minute: Int, second: Int): Boolean {
        return try {
            val startParts = def.windowStart.split(":")
            val endParts   = def.windowEnd.split(":")
            val nowSecs    = hour * 3600 + minute * 60 + second
            val startSecs  = startParts[0].toInt() * 3600 + startParts[1].toInt() * 60 + startParts[2].toInt()
            val endSecs    = endParts[0].toInt()   * 3600 + endParts[1].toInt()   * 60 + endParts[2].toInt()
            nowSecs in startSecs..endSecs
        } catch (_: Exception) { false }
    }
}
