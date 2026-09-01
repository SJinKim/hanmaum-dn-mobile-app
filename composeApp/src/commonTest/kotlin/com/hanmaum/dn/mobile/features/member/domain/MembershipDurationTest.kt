package com.hanmaum.dn.mobile.features.member.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MembershipDurationTest {

    private val today = LocalDate(2026, 9, 2)

    @Test
    fun countsWholeYearsAndLeftoverMonths() {
        assertEquals(
            MembershipDuration(years = 4, months = 6),
            membershipDuration("2022-03-02", today),
        )
    }

    @Test
    fun dropsTheMonthsOnAnExactAnniversary() {
        assertEquals(
            MembershipDuration(years = 4, months = 0),
            membershipDuration("2022-09-02", today),
        )
    }

    @Test
    fun reportsMonthsOnlyWithinTheFirstYear() {
        assertEquals(
            MembershipDuration(years = 0, months = 8),
            membershipDuration("2026-01-02", today),
        )
    }

    @Test
    fun isZeroOnTheDayOfRegistration() {
        assertEquals(
            MembershipDuration(years = 0, months = 0),
            membershipDuration("2026-09-02", today),
        )
    }

    @Test
    fun doesNotRoundAPartialMonthUp() {
        // One day short of a month is still zero months, not one.
        assertEquals(
            MembershipDuration(years = 0, months = 0),
            membershipDuration("2026-08-03", today),
        )
    }

    @Test
    fun isNullWhenTheMemberHasNoRegistrationDate() {
        assertNull(membershipDuration(null, today))
    }

    @Test
    fun isNullForAnUnparseableDate() {
        // The tile shows a dash rather than crashing the profile on bad data.
        assertNull(membershipDuration("not a date", today))
        assertNull(membershipDuration("", today))
    }

    @Test
    fun isNullForADateInTheFuture() {
        // A clock-skewed device must not render a negative membership.
        assertNull(membershipDuration("2026-09-03", today))
    }
}
