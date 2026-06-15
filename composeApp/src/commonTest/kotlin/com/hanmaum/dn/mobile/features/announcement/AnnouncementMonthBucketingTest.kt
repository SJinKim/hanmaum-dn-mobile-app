package com.hanmaum.dn.mobile.features.announcement

import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.presentation.AnnouncementSectionKey
import com.hanmaum.dn.mobile.features.announcement.presentation.bucketByMonth
import com.hanmaum.dn.mobile.features.announcement.presentation.monthIndexOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnnouncementMonthBucketingTest {

    private fun announcement(id: String, startAt: String): Announcement = Announcement(
        id = id,
        title = "t$id",
        body = "b$id",
        startAt = startAt,
        endAt = null,
        isPinned = false,
        category = "NOTICE",
    )

    // June 2026 -> 2026*12 + 6 = 24318
    private val june2026 = 2026 * 12 + 6

    @Test
    fun monthIndexParsesLeadingIsoDate() {
        assertEquals(2026 * 12 + 6, monthIndexOf("2026-06-14T10:00:00"))
        assertEquals(2026 * 12 + 1, monthIndexOf("2026-01-02"))
    }

    @Test
    fun monthIndexReturnsNullForGarbage() {
        assertNull(monthIndexOf("not-a-date"))
        assertNull(monthIndexOf(""))
    }

    @Test
    fun bucketsByThisLastAndEarlierMonths() {
        val items = listOf(
            announcement("a", "2026-06-20"), // this month
            announcement("b", "2026-06-01"), // this month
            announcement("c", "2026-05-15"), // last month
            announcement("d", "2026-03-10"), // earlier
            announcement("e", "2025-12-31"), // earlier
        )

        val sections = bucketByMonth(items, june2026)

        assertEquals(
            listOf(
                AnnouncementSectionKey.THIS_MONTH,
                AnnouncementSectionKey.LAST_MONTH,
                AnnouncementSectionKey.EARLIER,
            ),
            sections.map { it.key },
        )
        assertEquals(listOf("a", "b"), sections[0].items.map { it.id })
        assertEquals(listOf("c"), sections[1].items.map { it.id })
        assertEquals(listOf("d", "e"), sections[2].items.map { it.id })
    }

    @Test
    fun emptyBucketsAreOmitted() {
        val items = listOf(announcement("a", "2026-06-20"))
        val sections = bucketByMonth(items, june2026)
        assertEquals(1, sections.size)
        assertEquals(AnnouncementSectionKey.THIS_MONTH, sections[0].key)
    }

    @Test
    fun unparseableDatesFallIntoEarlier() {
        val items = listOf(announcement("x", "garbage"))
        val sections = bucketByMonth(items, june2026)
        assertEquals(1, sections.size)
        assertEquals(AnnouncementSectionKey.EARLIER, sections[0].key)
    }

    @Test
    fun futureDatedItemsCountAsThisMonth() {
        // A start date in a later month should still surface at the top, not vanish.
        val items = listOf(announcement("f", "2026-08-01"))
        val sections = bucketByMonth(items, june2026)
        assertTrue(sections.all { it.key == AnnouncementSectionKey.THIS_MONTH })
    }
}
