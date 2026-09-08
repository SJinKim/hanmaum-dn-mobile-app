package com.hanmaum.dn.mobile.features.verse.domain.repository

import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecordKind
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecords
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseStreak

interface VerseRecordRepository {
    /** Both streaks in one call, so Home does not load them twice. */
    suspend fun getRecords(): Result<VerseRecords>

    /**
     * Marks today for [kind] and returns that streak, refreshed.
     *
     * There is no way to undo a mark and no way to mark another day — the
     * server stamps the date and exposes no delete, so neither rule depends on
     * this client behaving.
     */
    suspend fun mark(kind: VerseRecordKind): Result<VerseStreak>
}
