package com.hanmaum.dn.mobile.core.location

/** How long a single fix may take before the caller gives up. */
const val LOCATION_TIMEOUT_MS: Long = 10_000

/**
 * One position fix, with the platform's own estimate of how wrong it might be.
 *
 * [accuracyMeters] is the radius of 68 % confidence around the coordinate — a
 * fix that is accurate to 500 m must not be allowed to answer "am I within
 * 100 m of the church?" with a yes. See [isConfidentlyWithin].
 */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
)

/**
 * The outcome of a single position request. The failure cases are kept apart
 * because the UI has to say something different for each: a missing permission
 * is fixable in the app, switched-off location services are fixable only in
 * system settings, and a timeout is worth retrying.
 */
sealed interface LocationResult {
    data class Success(val location: DeviceLocation) : LocationResult
    data object PermissionDenied : LocationResult
    data object LocationServicesDisabled : LocationResult
    data object Timeout : LocationResult
    data class Failed(val message: String?) : LocationResult
}

/**
 * A single "where am I right now" question.
 *
 * Deliberately separate from [com.hanmaum.dn.mobile.core.geofence.GeofenceManager],
 * which does region monitoring and reports *entering* the church. An entry
 * callback cannot answer this: someone who opens the app while already standing
 * inside the radius never produced an entry event.
 *
 * Implementations must not block longer than [LOCATION_TIMEOUT_MS].
 */
interface CurrentLocationProvider {
    suspend fun getCurrentLocation(): LocationResult
}
