package com.hanmaum.dn.mobile.features.verse.data.model

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /api/v1/verses/weekly`.
 *
 * The contract (`WeeklyVerseResponse` in `hanmaum-dn-ops/api/openapi.yaml`) also
 * carries `book`, `chapter`, `verseFrom`, `verseTo` and `sourceUrl`. They are left
 * out here because the card shows none of them, and the client is configured with
 * `ignoreUnknownKeys`. The streak gets its week from `/verses/records`, not from here.
 *
 * [weekStart] and [weekEnd] belong to the verse that was *delivered*, which is not
 * always the running week: with nothing published for this week the server answers
 * with the most recent verse it has (hanmaum-dn-server#160). The span is what keeps
 * that honest on the card, so it is read even though the other coordinates are not.
 * ISO dates as strings, matching `VerseRecordBlockDto.weekStart`.
 */
@Serializable
data class WeeklyVerseResponse(
    val reference: VerseReferenceDto? = null,
    val text: String? = null,
    val translation: String? = null,
    val weekStart: String? = null,
    val weekEnd: String? = null,
)
