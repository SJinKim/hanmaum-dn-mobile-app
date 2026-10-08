package com.hanmaum.dn.mobile.features.bulletin

import com.hanmaum.dn.mobile.core.domain.model.ChurchTimeZone
import com.hanmaum.dn.mobile.core.i18n.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BulletinStringsTest {
    @Test fun `question and song counts respect plurals and word order`() {
        assertEquals("1 question", EnBulletinStrings.questions(1))
        assertEquals("3 questions", EnBulletinStrings.questions(3))
        assertEquals("질문 3개", KoBulletinStrings.questions(3))
        assertEquals("1 Frage", DeBulletinStrings.questions(1))
        assertEquals("3 Fragen", DeBulletinStrings.questions(3))
        assertEquals("1 song", EnBulletinStrings.songCount(1))
        assertEquals("5곡", KoBulletinStrings.songCount(5))
        assertEquals("2 Lieder", DeBulletinStrings.songCount(2))
    }

    @Test fun `saved and published dates fit the language's sentence order`() {
        assertEquals("2026.10.10에 저장된 주보예요", KoBulletinStrings.offlineSince("2026.10.10"))
        assertEquals("2026.10.10 게시", KoBulletinStrings.publishedOn("2026.10.10"))
        assertEquals("Saved copy · saved on 10.10.2026", EnBulletinStrings.offlineSince("10.10.2026"))
        assertEquals("Veröffentlicht am 10.10.2026", DeBulletinStrings.publishedOn("10.10.2026"))
    }

    @Test fun `history keeps the cover's localized date order and includes the year`() {
        val date = LocalDate(2026, 10, 11)
        assertEquals("10.11", KoBulletinStrings.date(date))
        assertEquals("2026.10.11 · VOL. 41", KoBulletinStrings.historyCaption(date, 41))
        assertEquals("10.11.2026", EnBulletinStrings.historyCaption(date, null))
        assertEquals("11.10.2026 · VOL. 41", DeBulletinStrings.historyCaption(date, 41))
        // SUN and VOL are the deliberately shared labels on the existing Figma board.
        assertEquals("SUN", KoBulletinStrings.sunday)
    }

    @Test fun `service time is formatted from typed hours and minutes`() {
        assertEquals("3부 예배 · 14:00", KoBulletinStrings.serviceLine("3부 예배", LocalTime(14, 0, 30)))
        assertEquals("09:05", EnBulletinStrings.serviceLine(null, LocalTime(9, 5)))
        assertEquals("Service", DeBulletinStrings.serviceLine("Service", null))
    }

    @Test fun `church publication date remains consistent across the midnight boundary`() {
        val date = Instant.parse("2026-10-10T23:30:00Z").toLocalDateTime(ChurchTimeZone).date
        assertEquals(LocalDate(2026, 10, 11), date)
    }
}
