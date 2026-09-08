package com.hanmaum.dn.mobile.features.verse.data.model

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /api/v1/verses/weekly`.
 *
 * The contract (`WeeklyVerseResponse` in `hanmaum-dn-ops/api/openapi.yaml`) also
 * carries `book`, `chapter`, `verseFrom`, `verseTo`, `weekStart`, `weekEnd` and
 * `sourceUrl`. They are left out here because the card shows none of them, and
 * the client is configured with `ignoreUnknownKeys`. The streak gets its week
 * from `/verses/records`, not from here.
 */
@Serializable
data class WeeklyVerseResponse(
    val reference: VerseReferenceDto? = null,
    val text: String? = null,
    val translation: String? = null,
)
