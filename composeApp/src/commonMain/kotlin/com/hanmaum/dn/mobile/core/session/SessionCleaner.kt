package com.hanmaum.dn.mobile.core.session

import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.core.network.invalidateBearerCache
import com.hanmaum.dn.mobile.core.security.CredentialStore
import com.hanmaum.dn.mobile.features.geofence.domain.GeofenceCoordinator
import io.ktor.client.HttpClient

/**
 * Tears down everything that belongs to the signed-in member.
 *
 * Settings and the Keychain are device-global, not per-account, so anything
 * written while member A was signed in is still there when member B signs in on
 * the same device. Clearing tokens alone left B inheriting A's location-sharing
 * opt-in (so B was never asked), A's still-running geofence registration, and
 * A's bearer token cached inside the Ktor auth provider.
 *
 * One place for it, used by both exits from a session: an explicit logout and a
 * rejected/deleted account discovered at splash.
 */
class SessionCleaner(
    private val tokenStorage: TokenStorage,
    private val credentialStore: CredentialStore,
    private val locationPreferences: LocationPreferences,
    private val geofenceCoordinator: GeofenceCoordinator,
    private val httpClient: HttpClient,
) {
    fun clear() {
        tokenStorage.clear()
        // Signing out must also drop the saved credentials — otherwise the
        // next person at this device could biometric-unlock back in.
        credentialStore.clear()
        // Background monitoring was armed for the member who consented to it.
        geofenceCoordinator.disable()
        locationPreferences.clear()
        // The provider caches BearerTokens in memory; without this the next
        // member's first requests still carry the previous member's token.
        httpClient.invalidateBearerCache()
    }
}
