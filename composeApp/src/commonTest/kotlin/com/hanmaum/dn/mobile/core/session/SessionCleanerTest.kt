package com.hanmaum.dn.mobile.core.session

import com.hanmaum.dn.mobile.core.data.repository.LocationPreferencesImpl
import com.hanmaum.dn.mobile.core.data.repository.TokenStorageImpl
import com.hanmaum.dn.mobile.core.geofence.GeofenceManager
import com.hanmaum.dn.mobile.core.network.createHttpClient
import com.hanmaum.dn.mobile.core.notification.NotificationService
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.core.security.SecureStore
import com.hanmaum.dn.mobile.features.attendance.domain.repository.AttendanceRepository
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

private class InMemorySecureStore : SecureStore {
    private val map = mutableMapOf<String, String>()
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String): String? = map[key]
    override fun remove(key: String) { map.remove(key) }
}

private class RecordingGeofenceManager : GeofenceManager {
    var isMonitoring = false
    override fun isLocationPermissionGranted() = true
    override fun startMonitoring(location: ChurchLocation, onEnter: () -> Unit) { isMonitoring = true }
    override fun stopMonitoring() { isMonitoring = false }
}

private class StubNotificationService : NotificationService {
    override suspend fun isNotificationPermissionGranted() = true
    override suspend fun showAttendanceNotification() = Unit
}

private class StubAttendanceRepository : AttendanceRepository {
    override suspend fun getActiveDefinitions() = Result.success(emptyList<Nothing>())
    override suspend fun checkIn() = Result.success(Unit)
}

private class StubChurchLocationRepository : ChurchLocationRepository {
    override suspend fun getChurchLocation() = Result.success(ChurchLocation(37.0, 127.0, 100.0))
}

/**
 * Settings and the Keychain are device-global, so member A's state is still on
 * disk when member B signs in. These pin the properties that made B inherit it.
 */
class SessionCleanerTest {

    private fun fixture(settings: MapSettings, geofence: RecordingGeofenceManager) = SessionCleaner(
        tokenStorage = TokenStorageImpl(settings),
        credentialStore = CredentialStore(InMemorySecureStore()).also {
            it.saveCredentials("a@example.com", "Secret123!")
        },
        locationPreferences = LocationPreferencesImpl(settings),
        geofenceCoordinator = GeofenceCoordinator(
            StubChurchLocationRepository(),
            StubAttendanceRepository(),
            geofence,
            StubNotificationService(),
            LocationPreferencesImpl(settings),
        ),
        httpClient = createHttpClient(
            TokenStorageImpl(settings),
            MockEngine { respond("", HttpStatusCode.OK) },
        ),
    )

    @Test
    fun clearWipesTokensAndSavedCredentials() = runTest {
        val settings = MapSettings()
        val tokens = TokenStorageImpl(settings)
        tokens.saveAccessToken("access-a")
        tokens.saveRefreshToken("refresh-a")

        fixture(settings, RecordingGeofenceManager()).clear()

        assertNull(tokens.getAccessToken())
        assertNull(tokens.getRefreshToken())
    }

    @Test
    fun clearDropsTheLocationConsentSoTheNextMemberIsAskedAgain() = runTest {
        val settings = MapSettings()
        val prefs = LocationPreferencesImpl(settings)
        prefs.setSharingEnabled(true)
        prefs.setPromptDismissed(true)

        fixture(settings, RecordingGeofenceManager()).clear()

        assertFalse(prefs.isSharingEnabled(), "member B inherited member A's location opt-in")
        assertFalse(prefs.isPromptDismissed(), "member B would never be shown the prompt")
    }

    @Test
    fun clearStopsGeofenceMonitoringArmedForThePreviousMember() = runTest {
        val settings = MapSettings()
        LocationPreferencesImpl(settings).setSharingEnabled(true)
        val geofence = RecordingGeofenceManager()
        val cleaner = fixture(settings, geofence)
        geofence.startMonitoring(ChurchLocation(37.0, 127.0, 100.0)) {}

        cleaner.clear()

        assertFalse(geofence.isMonitoring)
    }
}
