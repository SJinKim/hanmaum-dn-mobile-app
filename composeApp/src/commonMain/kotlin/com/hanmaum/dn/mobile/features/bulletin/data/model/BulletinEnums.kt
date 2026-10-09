package com.hanmaum.dn.mobile.features.bulletin.data.model

import com.hanmaum.dn.mobile.features.bulletin.domain.model.BulletinSectionKey
import com.hanmaum.dn.mobile.features.bulletin.domain.model.SharingBlockType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BulletinStatusSerializer::class)
enum class BulletinStatus { DRAFT, PUBLISHED, WITHDRAWN, UNKNOWN }

/** Preserve forward compatibility locally without changing the shared client's JSON policy. */
private fun <T : Enum<T>> wireEnum(name: String, values: List<T>, unknown: T) = object : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): T = decoder.decodeString().let { wire ->
        values.find { it.name == wire } ?: unknown
    }
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.name)
}

object BulletinStatusSerializer : KSerializer<BulletinStatus> by
    wireEnum("BulletinStatus", BulletinStatus.entries, BulletinStatus.UNKNOWN)
object SharingBlockTypeSerializer : KSerializer<SharingBlockType> by
    wireEnum("BulletinSharingBlockType", SharingBlockType.entries, SharingBlockType.UNKNOWN)
object BulletinSectionKeySerializer : KSerializer<BulletinSectionKey> by
    wireEnum("BulletinSectionKey", BulletinSectionKey.entries, BulletinSectionKey.UNKNOWN)
