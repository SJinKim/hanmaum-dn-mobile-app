package com.hanmaum.dn.mobile.core.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiLocalizationTest {
    @Test fun everyTypedMessageHasEnglishAndKoreanCopy() {
        UiMessage.entries.forEach { message ->
            assertTrue(EnStrings.message(message).isNotBlank())
            assertTrue(KoStrings.message(message).isNotBlank())
            assertFalse(Regex("[가-힣]").containsMatchIn(EnStrings.message(message)))
            assertTrue(Regex("[가-힣]").containsMatchIn(KoStrings.message(message)))
        }
    }

    @Test fun aVisibleErrorIsResolvedUsingTheCurrentlySelectedLanguage() {
        val error = UiMessage.RSVP_WINDOW_CLOSED
        assertEquals("The response window has closed.", EnStrings.message(error))
        assertEquals("응답 기간이 지났습니다", KoStrings.message(error))
    }

    @Test fun nameExamplesAlwaysPairHongWithGildong() {
        listOf(EnStrings, KoStrings).forEach { strings ->
            assertTrue(strings.registerLastNameExample.contains("홍"))
            assertTrue(strings.registerFirstNameExample.contains("길동"))
            assertTrue(strings.registerKoreanNameHint.isNotBlank())
        }
        assertTrue(EnStrings.registerKoreanNameHint.contains("if possible"))
        assertTrue(KoStrings.registerKoreanNameHint.contains("가능하면"))
    }

    @Test fun datesAndEventCountsFollowTheAppLanguage() {
        assertEquals("10/22/2026", EnStrings.displayDate("2026-10-22T14:00:00"))
        assertEquals("2026년 10월 22일", KoStrings.displayDate("2026-10-22T14:00:00"))
        assertEquals("1 event", EnStrings.calendarEventCount(1))
        assertEquals("2 events", EnStrings.calendarEventCount(2))
        assertEquals("2개", KoStrings.calendarEventCount(2))
        assertEquals("unexpected", EnStrings.displayDate("unexpected"))
    }
}
