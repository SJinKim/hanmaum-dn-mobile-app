package com.hanmaum.dn.mobile.features.bulletin.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

data class Bulletin(
    val publicId: String,
    val serviceDate: LocalDate,
    val volume: Int?,
    val serviceName: String?,
    val serviceStartTime: LocalTime?,
    val openingPrayerBy: String?,
    val offeringSongBy: String?,
    val scriptureReference: String?,
    val sermonTitle: String,
    val sermonPreacher: String,
    val responsePrayerBy: String?,
    val responseSong: String?,
    val songs: List<String>,
    val announcements: List<BulletinAnnouncement>,
    val sharingBlocks: List<BulletinSharingBlock>,
    val sectionTitles: Map<BulletinSectionKey, String>,
    val publishedAt: Instant?,
) {
    fun sectionTitle(key: BulletinSectionKey): String = sectionTitles[key].orEmpty()
}

data class BulletinAnnouncement(val title: String, val body: String?)
enum class SharingBlockType { HEADING, PARAGRAPH, SCRIPTURE, QUESTION, UNKNOWN }
enum class BulletinSectionKey { SECTION_WORSHIP, SECTION_OFFERING, SECTION_SENDING, FIXED_BLESSING_PRAYER, UNKNOWN }
data class BulletinSharingBlock(val type: SharingBlockType, val text: String, val reference: String?)

/** Cached content is explicitly dated and never presented as a fresh server answer. */
data class BulletinRead(val bulletin: Bulletin, val cachedAt: Instant? = null)
data class BulletinSummary(val publicId: String, val serviceDate: LocalDate, val volume: Int?, val sermonTitle: String?)
data class BulletinPage(val editions: List<BulletinSummary>, val hasNext: Boolean)
