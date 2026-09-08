package com.hanmaum.dn.mobile.features.verse.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.verse.data.model.MarkVerseRecordRequest
import com.hanmaum.dn.mobile.features.verse.data.model.VerseRecordBlockDto
import com.hanmaum.dn.mobile.features.verse.data.model.VerseRecordsResponse
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecordKind
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecords
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseStreak
import com.hanmaum.dn.mobile.features.verse.domain.repository.VerseRecordRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

class VerseRecordRepositoryImpl(
    private val client: HttpClient,
) : VerseRecordRepository {

    override suspend fun getRecords(): Result<VerseRecords> = runCatching {
        val response = client.get("verses/records")
        if (response.status.value != 200) error("verses/records failed with ${response.status.value}")
        val body = response.body<ApiResponse<VerseRecordsResponse>>().data
            ?: error("verses/records returned no data")
        VerseRecords(
            quietTime = body.quietTime.toDomain(VerseRecordKind.QUIET_TIME),
            recitation = body.recitation.toDomain(VerseRecordKind.RECITATION),
        )
    }

    override suspend fun mark(kind: VerseRecordKind): Result<VerseStreak> = runCatching {
        val response = client.post("verses/records") {
            contentType(ContentType.Application.Json)
            setBody(MarkVerseRecordRequest(kind.name))
        }
        when (response.status.value) {
            200, 201 -> response.body<ApiResponse<VerseRecordBlockDto>>().data?.toDomain(kind)
                ?: error("verses/records returned no block")
            // Already marked today. The member's intent already holds, so this is a
            // success — re-reading the streak is the honest way to reflect it, since
            // the 409 body carries no block.
            409 -> getRecords().getOrThrow().of(kind)
            else -> error("marking $kind failed with ${response.status.value}")
        }
    }

    /**
     * A block the server did not send becomes an unmarkable, empty week rather
     * than an exception: one missing streak must not take Home's other card
     * down with it.
     */
    private fun VerseRecordBlockDto?.toDomain(kind: VerseRecordKind): VerseStreak {
        val dto = this ?: VerseRecordBlockDto()
        val start = dto.weekStart?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return VerseStreak(
            kind = kind,
            weekStart = start ?: LocalDate.fromEpochDays(0),
            markedDays = dto.days.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
            todayMarked = dto.todayMarked,
            // Without a week there is nothing to draw against, so nothing to mark.
            todayMarkable = dto.todayMarkable && start != null,
            totalDays = dto.totalDays,
        )
    }
}
