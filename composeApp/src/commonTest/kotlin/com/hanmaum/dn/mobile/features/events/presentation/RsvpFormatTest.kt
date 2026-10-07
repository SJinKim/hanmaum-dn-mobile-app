package com.hanmaum.dn.mobile.features.events.presentation

import com.hanmaum.dn.mobile.core.i18n.DeStrings
import com.hanmaum.dn.mobile.core.i18n.EnStrings
import com.hanmaum.dn.mobile.core.i18n.KoStrings
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RsvpFormatTest {
    private val date = Instant.parse("2025-08-30T12:00:00Z")

    @Test
    fun koreanLabelsPreserveExistingWording() {
        assertEquals("응답 마감 8월 30일 (토)", RsvpFormat.deadline(date, KoStrings, TimeZone.UTC))
        assertEquals("8월 30일 응답", RsvpFormat.respondedOn(date, KoStrings, TimeZone.UTC))
        assertEquals("미정 · 8월 30일에 한 번 더 알림", RsvpFormat.reminderHint(date, KoStrings, TimeZone.UTC))
    }

    @Test
    fun englishLabelsUseEnglishDates() {
        assertEquals("Reply by Sat, Aug 30", RsvpFormat.deadline(date, EnStrings, TimeZone.UTC))
        assertEquals("Replied on Aug 30", RsvpFormat.respondedOn(date, EnStrings, TimeZone.UTC))
        assertEquals("Undecided · One more reminder on Aug 30", RsvpFormat.reminderHint(date, EnStrings, TimeZone.UTC))
    }

    @Test
    fun germanLabelsUseGermanDates() {
        assertEquals("Antwort bis Sa., 30. Aug.", RsvpFormat.deadline(date, DeStrings, TimeZone.UTC))
        assertEquals("Geantwortet am 30. Aug.", RsvpFormat.respondedOn(date, DeStrings, TimeZone.UTC))
        assertEquals("Unentschieden · Eine weitere Erinnerung am 30. Aug.", RsvpFormat.reminderHint(date, DeStrings, TimeZone.UTC))
    }

    @Test
    fun dateUsesDeviceZoneRatherThanLanguageCountry() {
        val nearMidnight = Instant.parse("2025-08-30T22:30:00Z")
        assertEquals("Sun, Aug 31", RsvpFormat.date(nearMidnight, EnStrings, TimeZone.of("Europe/Berlin")))
        assertEquals("8월 30일 (토)", RsvpFormat.date(nearMidnight, KoStrings, TimeZone.UTC))
    }

    @Test
    fun germanMonthNamesAreNotNaivelyTruncated() {
        assertEquals("1. März", RsvpFormat.shortDate(Instant.parse("2025-03-01T12:00:00Z"), DeStrings, TimeZone.UTC))
        assertEquals("1. Sept.", RsvpFormat.shortDate(Instant.parse("2025-09-01T12:00:00Z"), DeStrings, TimeZone.UTC))
    }
}
