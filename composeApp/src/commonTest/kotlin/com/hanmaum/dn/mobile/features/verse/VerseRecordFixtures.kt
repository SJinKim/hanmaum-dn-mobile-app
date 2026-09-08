package com.hanmaum.dn.mobile.features.verse

import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecordKind
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecords
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseStreak
import com.hanmaum.dn.mobile.features.verse.domain.repository.VerseRecordRepository
import com.hanmaum.dn.mobile.features.verse.domain.model.withMark
import kotlinx.datetime.LocalDate

/** 2026-09-06 is a Sunday, so it is a valid week start for these streaks. */
val WEEK_START: LocalDate = LocalDate(2026, 9, 6)

/** Wednesday of [WEEK_START]'s week — a day both kinds can mark. */
val WEDNESDAY: LocalDate = LocalDate(2026, 9, 9)

fun streak(
    kind: VerseRecordKind,
    marked: Set<LocalDate> = emptySet(),
    todayMarked: Boolean = false,
    todayMarkable: Boolean = true,
    totalDays: Long = 0,
    weekStart: LocalDate = WEEK_START,
) = VerseStreak(
    kind = kind,
    weekStart = weekStart,
    markedDays = marked,
    todayMarked = todayMarked,
    todayMarkable = todayMarkable,
    totalDays = totalDays,
)

fun records(
    quietTime: VerseStreak = streak(VerseRecordKind.QUIET_TIME),
    recitation: VerseStreak = streak(VerseRecordKind.RECITATION),
) = VerseRecords(quietTime = quietTime, recitation = recitation)

class FakeVerseRecordRepository(
    private val initial: Result<VerseRecords> = Result.success(records()),
    private val markFails: Boolean = false,
) : VerseRecordRepository {
    val marked = mutableListOf<VerseRecordKind>()

    override suspend fun getRecords(): Result<VerseRecords> = initial

    override suspend fun mark(kind: VerseRecordKind): Result<VerseStreak> {
        marked += kind
        if (markFails) return Result.failure(RuntimeException("boom"))
        return initial.map { it.of(kind).withMark(WEDNESDAY) }
    }
}
