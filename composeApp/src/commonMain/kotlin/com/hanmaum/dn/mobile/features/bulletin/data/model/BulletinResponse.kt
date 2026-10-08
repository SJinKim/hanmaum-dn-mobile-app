package com.hanmaum.dn.mobile.features.bulletin.data.model

import com.hanmaum.dn.mobile.features.bulletin.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Camel-case fields copied from the server's BulletinEditionResponse (#146 / #291). */
@Serializable
data class BulletinResponse(
    val publicId: String,
    val serviceDate: String,
    val volume: Int? = null,
    val status: String,
    val servicePublicId: String,
    val serviceName: String? = null,
    val serviceStartTime: String? = null,
    val openingPrayerBy: String? = null,
    val offeringSongBy: String? = null,
    val scriptureReference: String? = null,
    val sermonTitle: String? = null,
    val sermonPreacher: String? = null,
    val responsePrayerBy: String? = null,
    val responseSong: String? = null,
    val songs: List<String> = emptyList(),
    val announcements: List<BulletinAnnouncementResponse> = emptyList(),
    val sharingBlocks: List<BulletinSharingBlockResponse> = emptyList(),
    val sectionTitles: List<BulletinSectionTitleResponse> = emptyList(),
    val publishedAt: String? = null,
    val withdrawnAt: String? = null,
    val version: Long,
) {
    fun toDomain(): Bulletin {
        check(status == "PUBLISHED" && withdrawnAt == null) { "Bulletin is not published" }
        return Bulletin(
            publicId, LocalDate.parse(serviceDate), volume, serviceName, serviceStartTime,
            openingPrayerBy, offeringSongBy, scriptureReference, sermonTitle.orEmpty(),
            sermonPreacher.orEmpty(), responsePrayerBy, responseSong, songs,
            announcements.map { BulletinAnnouncement(it.title, it.body) },
            // Unknown future block types are skipped; editorial text is always rendered as text.
            sharingBlocks.mapNotNull { block ->
                SharingBlockType.entries.find { it.name == block.type }?.let {
                    BulletinSharingBlock(it, block.text, block.reference)
                }
            },
            sectionTitles.associate { it.key to it.title },
            publishedAt?.let(Instant::parse),
        )
    }
}

@Serializable data class BulletinAnnouncementResponse(val title: String, val body: String? = null)
@Serializable data class BulletinSharingBlockResponse(val type: String, val text: String, val reference: String? = null)
@Serializable data class BulletinSectionTitleResponse(val key: String, val title: String, val defaultTitle: String? = null)
@Serializable data class BulletinSummaryResponse(
    val publicId: String,
    val serviceDate: String,
    val volume: Int? = null,
    val status: String,
    val sermonTitle: String? = null,
)
@Serializable data class BulletinPageResponse(val content: List<BulletinSummaryResponse>, val last: Boolean)
