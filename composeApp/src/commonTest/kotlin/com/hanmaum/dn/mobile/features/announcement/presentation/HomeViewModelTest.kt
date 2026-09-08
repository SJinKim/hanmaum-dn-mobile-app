package com.hanmaum.dn.mobile.features.announcement.presentation

import com.hanmaum.dn.mobile.core.push.PushManager
import com.hanmaum.dn.mobile.features.announcement.domain.model.Announcement
import com.hanmaum.dn.mobile.features.announcement.domain.model.AnnouncementLookup
import com.hanmaum.dn.mobile.features.announcement.domain.repository.AnnouncementRepository
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.repository.MemberRepository
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import com.hanmaum.dn.mobile.features.verse.domain.model.DailyVerse
import com.hanmaum.dn.mobile.features.verse.domain.model.WeeklyVerse
import com.hanmaum.dn.mobile.features.verse.domain.repository.VerseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAnnouncementRepository : AnnouncementRepository {
    override suspend fun getAnnouncements(): List<Announcement> = emptyList()
    override suspend fun getAnnouncementById(id: String): AnnouncementLookup = AnnouncementLookup.NotFound
}

private class FakeNotificationRepository(
    private val unseen: Int = 0,
    private val failCount: Boolean = false,
) : NotificationRepository {
    val registeredTokens = mutableListOf<Pair<String, String>>()

    override suspend fun getNotifications(page: Int) =
        Result.success(NotificationPage(emptyList(), hasNext = false))
    override suspend fun getUnseenCount() =
        if (failCount) Result.failure(RuntimeException("boom")) else Result.success(unseen)
    override suspend fun markAllSeen(): Result<Unit> = Result.success(Unit)
    override suspend fun markRead(publicId: String): Result<Unit> = Result.success(Unit)
    override suspend fun markAllRead(): Result<Unit> = Result.success(Unit)
    override suspend fun delete(publicId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteAll(): Result<Unit> = Result.success(Unit)
    override suspend fun getPushEnabled() = Result.success(true)
    override suspend fun setPushEnabled(enabled: Boolean) = Result.success(Unit)
    override suspend fun registerDeviceToken(token: String, platform: String): Result<Unit> {
        registeredTokens += token to platform
        return Result.success(Unit)
    }
    override suspend fun deleteDeviceToken(token: String): Result<Unit> = Result.success(Unit)
}

private class FakeMemberRepository : MemberRepository {
    override suspend fun getMyProfile(): Result<MemberResponse> =
        Result.failure(UnsupportedOperationException("not needed for these tests"))

    override suspend fun updateMyProfile(
        phoneNumber: String?,
        profileImageUrl: String?,
        birthDate: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
    ): Result<MemberResponse> = Result.failure(UnsupportedOperationException("not needed for these tests"))
}

private class FakeVerseRepository(
    private val result: Result<DailyVerse?> = Result.success(null),
    private val weekly: Result<WeeklyVerse?> = Result.success(null),
) : VerseRepository {
    override suspend fun getTodayVerse(): Result<DailyVerse?> = result
    override suspend fun getWeeklyVerse(): Result<WeeklyVerse?> = weekly
}

private class FakePushManager(private val token: String?) : PushManager {
    override val platform: String = "ANDROID"
    override suspend fun currentToken(): String? = token
    override fun isPermissionGranted(): Boolean = true
    override suspend fun requestPermission(): Boolean = true
}

class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `unseen count lands in ui state`() = runTest(dispatcher) {
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(unseen = 5),
            FakeMemberRepository(),
            FakeVerseRepository(),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(5, vm.uiState.value.unseenCount)
    }

    @Test
    fun `unseen count failure keeps zero`() = runTest(dispatcher) {
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(failCount = true),
            FakeMemberRepository(),
            FakeVerseRepository(),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.unseenCount)
    }

    @Test
    fun `registers device token on load when available`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakeMemberRepository(), FakeVerseRepository(), FakePushManager(token = "tok1"))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(listOf("tok1" to "ANDROID"), repo.registeredTokens)
    }

    @Test
    fun `null token skips registration`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakeMemberRepository(), FakeVerseRepository(), FakePushManager(token = null))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertTrue(repo.registeredTokens.isEmpty())
    }

    @Test
    fun `token registers only once per process`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakeMemberRepository(), FakeVerseRepository(), FakePushManager(token = "tok1"))
        vm.loadAnnouncements(); advanceUntilIdle()
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(1, repo.registeredTokens.size)
    }

    @Test
    fun `daily verse lands in ui state`() = runTest(dispatcher) {
        val verse = DailyVerse(
            referenceKo = "신명기 3:1-11",
            referenceEn = "Deuteronomy 3:1-11",
            translation = "개역개정",
            sourceUrl = "https://bible.asher.design/quiettime.php?qt_date=2026-09-08",
        )
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(),
            FakeMemberRepository(),
            FakeVerseRepository(Result.success(verse)),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(verse, vm.uiState.value.dailyVerse)
    }

    @Test
    fun `a day without a passage leaves the card hidden`() = runTest(dispatcher) {
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(),
            FakeMemberRepository(),
            FakeVerseRepository(Result.success(null)),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertNull(vm.uiState.value.dailyVerse)
    }

    @Test
    fun `a failing verse call leaves home without an error`() = runTest(dispatcher) {
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(),
            FakeMemberRepository(),
            FakeVerseRepository(Result.failure(RuntimeException("boom"))),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertNull(vm.uiState.value.dailyVerse)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `weekly verse lands in ui state`() = runTest(dispatcher) {
        val verse = WeeklyVerse(
            reference = "시편 23:1",
            text = "여호와는 나의 목자시니 내게 부족함이 없으리로다",
            translation = "개역개정",
        )
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(),
            FakeMemberRepository(),
            FakeVerseRepository(weekly = Result.success(verse)),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(verse, vm.uiState.value.weeklyVerse)
    }

    @Test
    fun `no weekly verse set leaves that card hidden`() = runTest(dispatcher) {
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(),
            FakeMemberRepository(),
            FakeVerseRepository(weekly = Result.success(null)),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertNull(vm.uiState.value.weeklyVerse)
    }

    @Test
    fun `a failing weekly call still lets the daily passage through`() = runTest(dispatcher) {
        // The two calls are independent on purpose: one card going missing must not
        // take the other with it.
        val daily = DailyVerse("신명기 3:1-11", "Deuteronomy 3:1-11", "개역개정", null)
        val vm = HomeViewModel(
            FakeAnnouncementRepository(),
            FakeNotificationRepository(),
            FakeMemberRepository(),
            FakeVerseRepository(
                result = Result.success(daily),
                weekly = Result.failure(RuntimeException("boom")),
            ),
            FakePushManager(token = null),
        )
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(daily, vm.uiState.value.dailyVerse)
        assertNull(vm.uiState.value.weeklyVerse)
        assertNull(vm.uiState.value.error)
    }
}
