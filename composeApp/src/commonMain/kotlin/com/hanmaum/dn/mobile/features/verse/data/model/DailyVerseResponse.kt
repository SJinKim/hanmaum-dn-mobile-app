package com.hanmaum.dn.mobile.features.verse.data.model

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /api/v1/verses/today`, per the contract agreed in
 * hanmaum-dn-server#115. Every field is optional so a partially-filled row
 * degrades to a hidden card instead of a parse failure.
 */
@Serializable
data class DailyVerseResponse(
    val reference: VerseReferenceDto? = null,
    val translation: String? = null,
    val sourceUrl: String? = null,
)

@Serializable
data class VerseReferenceDto(
    val ko: String? = null,
    val en: String? = null,
    val de: String? = null,
)
