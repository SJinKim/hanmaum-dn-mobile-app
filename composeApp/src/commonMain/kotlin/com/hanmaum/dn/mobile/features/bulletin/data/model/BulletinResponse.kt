package com.hanmaum.dn.mobile.features.bulletin.data.model

import com.hanmaum.dn.mobile.features.bulletin.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Camel-case fields copied from the server's BulletinEditionResponse (#146 / #291). */
@Serializable
data class BulletinResponse(
    val publicId: String,
    val serviceDate: String,
    val volume: Int? = null,
    val status: BulletinStatus,
    val servicePublicId: String,
    val serviceName: String? = null,
    val serviceStartTime: LocalTime? = null,
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
    fun toDomainOrNull(): Bulletin? {
        if (status != BulletinStatus.PUBLISHED || withdrawnAt != null) return null
        return Bulletin(
            publicId, LocalDate.parse(serviceDate), volume, serviceName, serviceStartTime,
            openingPrayerBy, offeringSongBy, scriptureReference, sermonTitle.orEmpty(),
            sermonPreacher.orEmpty(), responsePrayerBy, responseSong, songs,
            announcements.map { BulletinAnnouncement(it.title, it.body) },
            // Unknown future block types are skipped; editorial text is always rendered as text.
            sharingBlocks.mapNotNull { block ->
                block.type.takeUnless { it == SharingBlockType.UNKNOWN }?.let {
                    BulletinSharingBlock(it, block.text, block.reference)
                }
            },
            sectionTitles.filter { it.key != BulletinSectionKey.UNKNOWN }
                .associate { it.key to it.title.ifBlank { it.defaultTitle } },
            publishedAt?.let(Instant::parse),
        )
    }
}

@Serializable data class BulletinAnnouncementResponse(val title: String, val body: String? = null)
@Serializable data class BulletinSharingBlockResponse(
    @Serializable(with = SharingBlockTypeSerializer::class) val type: SharingBlockType,
    val text: String,
    val reference: String? = null,
)
@Serializable data class BulletinSectionTitleResponse(
    @Serializable(with = BulletinSectionKeySerializer::class) val key: BulletinSectionKey,
    val title: String,
    val defaultTitle: String,
)
@Serializable data class BulletinSummaryResponse(
    val publicId: String,
    val serviceDate: String,
    val volume: Int? = null,
    val status: BulletinStatus,
    val sermonTitle: String? = null,
)
@Serializable data class BulletinPageResponse(val content: List<BulletinSummaryResponse>, val last: Boolean)
