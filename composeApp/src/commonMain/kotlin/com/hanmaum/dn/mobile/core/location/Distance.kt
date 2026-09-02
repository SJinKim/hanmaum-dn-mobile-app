package com.hanmaum.dn.mobile.core.location

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** IUGG mean Earth radius, in metres. */
private const val EARTH_RADIUS_METERS = 6_371_008.8

private const val DEG_TO_RAD = 0.017453292519943295

/**
 * Great-circle distance in metres between two coordinates.
 *
 * Haversine on a sphere. At the scale this is used for — a 100 m radius around
 * one church — the error against a proper ellipsoidal model is well under a
 * metre, far below any consumer GPS fix.
 */
fun distanceMeters(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double,
): Double {
    val dLat = (toLatitude - fromLatitude) * DEG_TO_RAD
    val dLon = (toLongitude - fromLongitude) * DEG_TO_RAD
    val sinLat = sin(dLat / 2)
    val sinLon = sin(dLon / 2)

    val a = sinLat * sinLat +
        cos(fromLatitude * DEG_TO_RAD) * cos(toLatitude * DEG_TO_RAD) * sinLon * sinLon

    // min(1.0, …) guards asin against a value a hair above 1 from rounding.
    return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
}

/** Distance from this fix to the centre of [church], in metres. */
fun DeviceLocation.distanceTo(church: ChurchLocation): Double =
    distanceMeters(latitude, longitude, church.latitude, church.longitude)

/**
 * True only when the fix is inside the radius *even at its worst case*.
 *
 * The accuracy is added to the distance rather than ignored: a point measured
 * 90 m from the centre with a 500 m error tells us nothing about being inside a
 * 100 m radius, and answering "yes" there would let someone check in from home.
 * The cost is that a poor fix reads as outside — the safe direction to be wrong
 * for an attendance check-in.
 */
fun DeviceLocation.isConfidentlyWithin(church: ChurchLocation): Boolean =
    distanceTo(church) + accuracyMeters <= church.radiusMeters
