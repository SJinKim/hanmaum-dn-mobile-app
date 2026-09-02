package com.hanmaum.dn.mobile.core.location

import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The reference values are properties of a sphere, not numbers read back out of
 * this implementation: one degree of arc is R·π/180 and half a circumference is
 * R·π. Asserting against those catches a wrong radius, a degree/radian mix-up
 * and a swapped latitude/longitude, which re-deriving the expected value from
 * the same formula would not.
 */
class DistanceTest {

    // pi*R/180 and pi*R for R = 6_371_008.8 m.
    private val oneDegreeOfArc = 111_195.08
    private val halfCircumference = 20_015_114.44

    @Test
    fun distanceIsZeroForTheSamePoint() {
        assertEquals(0.0, distanceMeters(50.9413, 6.9583, 50.9413, 6.9583))
    }

    @Test
    fun oneDegreeOfLatitudeIsOneDegreeOfArc() {
        val d = distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertNear(oneDegreeOfArc, d, tolerance = 1.0)
    }

    @Test
    fun oneDegreeOfLongitudeAtTheEquatorIsOneDegreeOfArc() {
        val d = distanceMeters(0.0, 0.0, 0.0, 1.0)
        assertNear(oneDegreeOfArc, d, tolerance = 1.0)
    }

    @Test
    fun oneDegreeOfLongitudeShrinksAwayFromTheEquator() {
        // At 60° north a degree of longitude is half of one at the equator
        // (cos 60° = 0.5). A latitude/longitude swap would break this.
        val d = distanceMeters(60.0, 0.0, 60.0, 1.0)
        assertNear(oneDegreeOfArc / 2, d, tolerance = 100.0)
    }

    @Test
    fun poleToPoleIsHalfACircumference() {
        val d = distanceMeters(90.0, 0.0, -90.0, 0.0)
        assertNear(halfCircumference, d, tolerance = 1.0)
    }

    @Test
    fun distanceIsSymmetric() {
        val there = distanceMeters(50.9413, 6.9583, 50.9430, 6.9590)
        val back = distanceMeters(50.9430, 6.9590, 50.9413, 6.9583)
        assertNear(there, back, tolerance = 0.001)
    }

    @Test
    fun resolvesTheHundredMetreScale() {
        // A thousandth of a degree of latitude is about 111 m — the scale the
        // church radius actually works at.
        val d = distanceMeters(50.9413, 6.9583, 50.9423, 6.9583)
        assertNear(111.19, d, tolerance = 0.5)
    }

    // ── the accuracy rule ────────────────────────────────────────────────

    private val church = ChurchLocation(latitude = 50.9413, longitude = 6.9583, radiusMeters = 100.0)

    @Test
    fun aGoodFixAtTheCentreIsWithin() {
        val fix = DeviceLocation(50.9413, 6.9583, accuracyMeters = 10.0)
        assertTrue(fix.isConfidentlyWithin(church))
    }

    @Test
    fun aFixWellOutsideTheRadiusIsNotWithin() {
        val fix = DeviceLocation(50.9450, 6.9583, accuracyMeters = 10.0)
        assertFalse(fix.isConfidentlyWithin(church))
    }

    @Test
    fun aVagueFixAtTheCentreIsNotWithin() {
        // Standing exactly on the centre but with a 500 m error: the true
        // position could be anywhere in a 500 m disc, so this must not pass.
        val fix = DeviceLocation(50.9413, 6.9583, accuracyMeters = 500.0)
        assertFalse(fix.isConfidentlyWithin(church), "a 500 m error cannot answer a 100 m question")
    }

    @Test
    fun accuracyCountsAgainstTheRadius() {
        // ~55 m from the centre. Fine with a 20 m error, not with a 50 m one.
        val near = DeviceLocation(50.94180, 6.9583, accuracyMeters = 20.0)
        val same = DeviceLocation(50.94180, 6.9583, accuracyMeters = 50.0)
        assertTrue(near.distanceTo(church) < 100.0, "fixture must sit inside the radius")
        assertTrue(near.isConfidentlyWithin(church))
        assertFalse(same.isConfidentlyWithin(church))
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected +/- $tolerance but was $actual",
        )
    }
}
