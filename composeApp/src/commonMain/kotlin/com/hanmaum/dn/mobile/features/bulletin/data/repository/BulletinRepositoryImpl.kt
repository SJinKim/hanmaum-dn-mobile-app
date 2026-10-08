package com.hanmaum.dn.mobile.features.bulletin.data.repository

import com.hanmaum.dn.mobile.core.security.currentSessionScope
import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.core.domain.repository.TokenStorage
import com.hanmaum.dn.mobile.features.bulletin.data.model.*
import com.hanmaum.dn.mobile.features.bulletin.domain.model.*
import com.hanmaum.dn.mobile.features.bulletin.domain.repository.BulletinRepository
import com.hanmaum.dn.mobile.features.bulletin.domain.repository.BulletinAccessDenied
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
private data class SavedBulletin(val response: BulletinResponse, val savedAt: String)
@Serializable
private data class BulletinCache(val owner: String, val currentId: String? = null, val editions: List<SavedBulletin> = emptyList())
private class BulletinHttpFailure(val status: Int) : Exception("Bulletin request failed ($status)")

/**
 * A bounded, account/environment-scoped cache for short outages (24 hours).
 * 404 and access denial invalidate cached content; only transport/5xx failures use it.
 * Offline devices cannot discover a withdrawal until the next successful request.
 */
class BulletinRepositoryImpl(
    private val client: HttpClient,
    private val settings: Settings,
    private val tokenStorage: TokenStorage,
    private val now: () -> Instant = { Clock.System.now() },
) : BulletinRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    // Long editions can make JSON/cache processing sizeable; keep it off the UI thread.
    override suspend fun getCurrent(): Result<BulletinRead?> = withContext(Dispatchers.Default) { read(null) }
    override suspend fun getByDate(date: LocalDate): Result<BulletinRead?> = withContext(Dispatchers.Default) { read(date) }
    override suspend fun getHistory(page: Int): Result<BulletinPage> = withContext(Dispatchers.Default) { history(page) }

    private suspend fun read(date: LocalDate?): Result<BulletinRead?> = mutex.withLock {
        val owner = owner()
        try {
            val response = client.get(if (date == null) "bulletins/current" else "bulletins") {
                date?.let { parameter("date", it.toString()) }
            }
            check(owner == owner()) { "Session changed" }
            when (response.status.value) {
                404 -> {
                    invalidate(owner, date)
                    Result.success(null)
                }
                200 -> {
                    val envelope = response.body<ApiResponse<BulletinResponse>>()
                    check(envelope.success) { "Bulletin response failed" }
                    val dto = requireNotNull(envelope.data) { "Missing bulletin content" }
                    val bulletin = dto.toDomainOrNull()
                    if (bulletin == null) {
                        invalidate(owner, date)
                        return@withLock Result.success(null)
                    }
                    check(date == null || bulletin.serviceDate == date) { "Unexpected bulletin date" }
                    save(owner, dto, date == null)
                    Result.success(BulletinRead(bulletin))
                }
                else -> throw BulletinHttpFailure(response.status.value)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is BulletinHttpFailure && e.status in listOf(401, 403)) {
                clear()
                return@withLock Result.failure(BulletinAccessDenied())
            }
            val transient = (e is BulletinHttpFailure && e.status >= 500) ||
                e is kotlinx.io.IOException || e is HttpRequestTimeoutException
            val cached = if (transient && owner == owner()) cached(owner, date) else null
            cached?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    private suspend fun history(page: Int): Result<BulletinPage> = mutex.withLock {
        val owner = owner()
        try {
            val response = client.get("bulletins/history") {
                parameter("page", page.coerceAtLeast(0))
                parameter("size", 20)
            }
            check(owner == owner()) { "Session changed" }
            if (response.status.value != 200) throw BulletinHttpFailure(response.status.value)
            val envelope = response.body<ApiResponse<BulletinPageResponse>>()
            check(envelope.success) { "Bulletin history response failed" }
            val data = requireNotNull(envelope.data)
            Result.success(BulletinPage(
                data.content.filter { it.status == BulletinStatus.PUBLISHED }.map {
                    BulletinSummary(it.publicId, LocalDate.parse(it.serviceDate), it.volume, it.sermonTitle)
                }, !data.last,
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is BulletinHttpFailure && e.status in listOf(401, 403)) {
                clear()
                return@withLock Result.failure(BulletinAccessDenied())
            }
            Result.failure(e)
        }
    }

    private fun owner(): String? = tokenStorage.currentSessionScope()

    private fun cache(owner: String?): BulletinCache? = runCatching {
        settings.getStringOrNull(CACHE_KEY)?.let { json.decodeFromString<BulletinCache>(it) }
            ?.takeIf { owner != null && it.owner == owner }
    }.getOrNull()

    private fun cached(owner: String?, date: LocalDate?): BulletinRead? = runCatching {
        val cache = cache(owner) ?: return null
        val entry = cache.editions.find {
            if (date == null) it.response.publicId == cache.currentId else it.response.serviceDate == date.toString()
        } ?: return null
        val savedAt = Instant.parse(entry.savedAt)
        val age = now().epochSeconds - savedAt.epochSeconds
        if (age !in 0..CACHE_SECONDS) return null
        entry.response.toDomainOrNull()?.let { BulletinRead(it, savedAt) }
    }.getOrNull()

    private fun save(owner: String?, dto: BulletinResponse, current: Boolean) {
        if (owner == null) return
        val before = cache(owner) ?: BulletinCache(owner)
        val currentId = if (current) dto.publicId else before.currentId
        val updated = listOf(SavedBulletin(dto, now().toString())) +
            before.editions.filter { it.response.publicId != dto.publicId }
        val currentEntry = updated.find { it.response.publicId == currentId }
        write(before.copy(currentId = currentId, editions =
            (listOfNotNull(currentEntry) + updated.filter { it.response.publicId != currentId }).take(20)))
    }

    private fun invalidate(owner: String?, date: LocalDate?) {
        val before = cache(owner) ?: return
        val removedIds = before.editions.filter {
            if (date == null) it.response.publicId == before.currentId else it.response.serviceDate == date.toString()
        }.map { it.response.publicId }.toSet()
        write(before.copy(
            currentId = before.currentId?.takeUnless { it in removedIds },
            editions = before.editions.filterNot { it.response.publicId in removedIds },
        ))
    }

    private fun write(cache: BulletinCache) { runCatching { settings.putString(CACHE_KEY, json.encodeToString(cache)) } }
    private fun clear() { runCatching { settings.remove(CACHE_KEY) } }

    companion object {
        private const val CACHE_KEY = "bulletin_cache_v1"
        private const val CACHE_SECONDS = 24 * 60 * 60L
    }
}
