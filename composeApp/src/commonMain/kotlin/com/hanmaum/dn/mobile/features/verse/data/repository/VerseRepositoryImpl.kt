package com.hanmaum.dn.mobile.features.verse.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.verse.data.model.DailyVerseResponse
import com.hanmaum.dn.mobile.features.verse.data.model.WeeklyVerseResponse
import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerse
import com.hanmaum.dn.mobile.features.verse.domain.model.WeeklyVerse
import com.hanmaum.dn.mobile.features.verse.domain.repository.VerseRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class VerseRepositoryImpl(
    private val client: HttpClient,
) : VerseRepository {

    // The shared client uses expectSuccess = false, so non-2xx returns normally
    // and we branch on the status code instead of catching Ktor exceptions.
    override suspend fun getTodayVerse(): Result<DailyVerse?> = runCatching {
        val response = client.get("verses/today")
        when (response.status.value) {
            200 -> response.body<ApiResponse<DailyVerseResponse>>().data?.toDomainOrNull()
            // No passage planned for today — a Sunday, or a year not written yet.
            204, 404 -> null
            else -> error("verses/today failed with ${response.status.value}")
        }
    }

    override suspend fun getWeeklyVerse(): Result<WeeklyVerse?> = runCatching {
        val response = client.get("verses/weekly")
        when (response.status.value) {
            200 -> response.body<ApiResponse<WeeklyVerseResponse>>().data?.toDomainOrNull()
            204, 404 -> null
            // 503 means the church's bible source could not be reached. The server
            // distinguishes that from "no verse set" on purpose, so it must not be
            // flattened into null here — a retry may well succeed.
            else -> error("verses/weekly failed with ${response.status.value}")
        }
    }

    /**
     * Needs both a reference and a text. The card's whole content is the verse,
     * so half a row would render as an empty card with a heading.
     */
    private fun WeeklyVerseResponse.toDomainOrNull(): WeeklyVerse? {
        val reference = reference?.ko?.trim().orEmpty()
        val body = text?.trim().orEmpty()
        if (reference.isEmpty() || body.isEmpty()) return null
        return WeeklyVerse(
            reference = reference,
            text = body,
            translation = translation?.trim().orEmpty(),
        )
    }

    /**
     * Drops a row that carries no usable reference. Without one the card has
     * nothing to show, and an empty card is worse than no card.
     */
    private fun DailyVerseResponse.toDomainOrNull(): DailyVerse? {
        val ko = reference?.ko?.trim().orEmpty()
        val en = reference?.en?.trim().orEmpty()
        if (ko.isEmpty() && en.isEmpty()) return null
        return DailyVerse(
            referenceKo = ko,
            referenceEn = en,
            translation = translation?.trim().orEmpty(),
            sourceUrl = sourceUrl?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}
