package com.hanmaum.dn.mobile.features.events.presentation

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
    fun dateUsesDeviceZoneRatherThanLanguageCountry() {
        val nearMidnight = Instant.parse("2025-08-30T22:30:00Z")
        assertEquals("Sun, Aug 31", RsvpFormat.date(nearMidnight, EnStrings, TimeZone.of("Europe/Berlin")))
        assertEquals("8월 30일 (토)", RsvpFormat.date(nearMidnight, KoStrings, TimeZone.UTC))
    }

}
