package com.hanmaum.dn.mobile.core.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A member's account state, as the server reports it.
 *
 * [REJECTED] is deprecated server-side but still reachable on historical rows,
 * and [UNKNOWN] catches anything added later. Neither may be dropped: the
 * status arrives on `/members/me`, which splash, login and the pending screen
 * all depend on — a value the enum does not know would fail the whole response
 * and leave the member with an app that cannot start.
 */
@Serializable(with = MemberStatusSerializer::class)
enum class MemberStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
    DELETED,

    /** No longer issued; historical rows still carry it. */
    REJECTED,

    /** Anything this build does not recognise. Treated as "not usable". */
    UNKNOWN;

    companion object {
        fun fromWire(raw: String?): MemberStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

/**
 * Maps an unrecognised status to [MemberStatus.UNKNOWN] instead of throwing.
 *
 * Deliberately narrow: coercing globally via `coerceInputValues` would silence
 * every enum in every DTO, hiding real contract drift. Only this one field is
 * load-bearing enough to warrant failing soft — and it fails *closed*, because
 * everything except ACTIVE and PENDING routes to tearing the session down.
 */
object MemberStatusSerializer : KSerializer<MemberStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.hanmaum.dn.mobile.core.domain.model.MemberStatus", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): MemberStatus = MemberStatus.fromWire(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: MemberStatus) = encoder.encodeString(value.name)
}
