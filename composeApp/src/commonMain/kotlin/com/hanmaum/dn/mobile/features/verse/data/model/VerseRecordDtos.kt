package com.hanmaum.dn.mobile.features.verse.data.model

import kotlinx.serialization.Serializable

/** Wire shape of `VerseRecordBlock` in `hanmaum-dn-ops/api/openapi.yaml`. */
@Serializable
data class VerseRecordBlockDto(
    val weekStart: String? = null,
    val days: List<String> = emptyList(),
    val todayMarked: Boolean = false,
    val todayMarkable: Boolean = false,
    val totalDays: Long = 0,
)

@Serializable
data class VerseRecordsResponse(
    val quietTime: VerseRecordBlockDto? = null,
    val recitation: VerseRecordBlockDto? = null,
)

/** Body of `POST /api/v1/verses/records`. Carries no date — the server stamps it. */
@Serializable
data class MarkVerseRecordRequest(
    val kind: String,
)
