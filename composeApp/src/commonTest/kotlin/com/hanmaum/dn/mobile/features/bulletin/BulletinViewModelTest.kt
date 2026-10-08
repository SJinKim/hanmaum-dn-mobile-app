package com.hanmaum.dn.mobile.features.bulletin

import com.hanmaum.dn.mobile.features.bulletin.domain.model.*
import com.hanmaum.dn.mobile.features.bulletin.domain.repository.BulletinRepository
import com.hanmaum.dn.mobile.features.bulletin.domain.repository.BulletinAccessDenied
import com.hanmaum.dn.mobile.features.bulletin.presentation.BulletinViewModel
import com.hanmaum.dn.mobile.features.bulletin.presentation.bulletinDate
import com.hanmaum.dn.mobile.features.bulletin.presentation.numberedSharingBlocks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Instant

private class FakeBulletinRepository : BulletinRepository {
    var current: Result<BulletinRead?> = Result.success(BulletinRead(bulletin()))
    var dated = current
    var waitForCurrent: CompletableDeferred<Unit>? = null
    val dates = mutableListOf<LocalDate>()
    var historyResult = Result.success(BulletinPage(emptyList(), false))
    val pages = mutableListOf<Int>()
    override suspend fun getCurrent(): Result<BulletinRead?> {
        waitForCurrent?.await()
        return current
    }
    override suspend fun getByDate(date: LocalDate): Result<BulletinRead?> { dates += date; return dated }
    override suspend fun getHistory(page: Int): Result<BulletinPage> { pages += page; return historyResult }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BulletinViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { Dispatchers.resetMain() }

    @Test fun `initial load never flashes the empty state`() = runTest(dispatcher) {
        val vm = BulletinViewModel(FakeBulletinRepository())
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Begin again", vm.uiState.value.content?.bulletin?.sermonTitle)
    }

    @Test fun `no publication is different from failure and retry recovers`() = runTest(dispatcher) {
        val repository = FakeBulletinRepository().apply { current = Result.success(null) }
        val vm = BulletinViewModel(repository)
        advanceUntilIdle()
        assertNull(vm.uiState.value.content)
        assertFalse(vm.uiState.value.failed)
        repository.current = Result.failure(Exception("server down"))
        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.failed)
        repository.current = Result.success(BulletinRead(bulletin()))
        vm.refresh()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.failed)
        assertNotNull(vm.uiState.value.content)
    }

    @Test fun `refresh keeps content while in flight but removes it on definitive failure`() = runTest(dispatcher) {
        val repository = FakeBulletinRepository()
        val vm = BulletinViewModel(repository)
        advanceUntilIdle()
        repository.waitForCurrent = CompletableDeferred()
        repository.current = Result.failure(Exception("access denied"))
        vm.refresh()
        runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.content)
        repository.waitForCurrent!!.complete(Unit)
        advanceUntilIdle()
        assertNull(vm.uiState.value.content)
        assertTrue(vm.uiState.value.failed)
    }

    @Test fun `cached response carries its date without pretending refresh succeeded online`() = runTest(dispatcher) {
        val saved = Instant.parse("2026-10-10T11:00:00Z")
        val repository = FakeBulletinRepository().apply { current = Result.success(BulletinRead(bulletin(), saved)) }
        val vm = BulletinViewModel(repository)
        advanceUntilIdle()
        assertEquals(saved, vm.uiState.value.content?.cachedAt)
        assertFalse(vm.uiState.value.failed)
    }

    @Test fun `selecting history closes selector and refresh keeps that selection`() = runTest(dispatcher) {
        val repository = FakeBulletinRepository()
        val vm = BulletinViewModel(repository)
        advanceUntilIdle()
        vm.openHistory()
        advanceUntilIdle()
        val date = LocalDate(2026, 10, 4)
        vm.selectTab(1)
        vm.selectEdition(date)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.historyOpen)
        assertEquals(0, vm.uiState.value.selectedTab)
        assertEquals(date, vm.uiState.value.selectedDate)
        vm.selectTab(1)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf(date, date), repository.dates)
        assertEquals(1, vm.uiState.value.selectedTab)
        vm.selectEdition(null)
        advanceUntilIdle()
        assertNull(vm.uiState.value.selectedDate)
    }

    @Test fun `slow current load cannot overwrite a later history selection`() = runTest(dispatcher) {
        val repository = FakeBulletinRepository().apply { waitForCurrent = CompletableDeferred() }
        val vm = BulletinViewModel(repository)
        runCurrent()
        val date = LocalDate(2026, 10, 4)
        repository.dated = Result.success(BulletinRead(bulletin().copy(serviceDate = date)))
        vm.selectEdition(date)
        advanceUntilIdle()
        repository.waitForCurrent!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(date, vm.uiState.value.content?.bulletin?.serviceDate)
    }

    @Test fun `history pagination deduplicates and failed pages retry without skipping`() = runTest(dispatcher) {
        val row = BulletinSummary("one", LocalDate(2026, 10, 11), 41, "First")
        val repository = FakeBulletinRepository().apply { historyResult = Result.success(BulletinPage(listOf(row), true)) }
        val vm = BulletinViewModel(repository)
        vm.openHistory()
        vm.loadMoreHistory()
        advanceUntilIdle()
        assertEquals(listOf(0), repository.pages)
        repository.historyResult = Result.failure(Exception("offline"))
        vm.loadMoreHistory()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.historyFailed)
        assertEquals(listOf(row), vm.uiState.value.history)
        repository.historyResult = Result.success(BulletinPage(listOf(row, row.copy(publicId = "two")), false))
        vm.loadMoreHistory()
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 1), repository.pages)
        assertEquals(listOf("one", "two"), vm.uiState.value.history.map { it.publicId })
        assertFalse(vm.uiState.value.historyHasNext)
    }

    @Test fun `question numbers follow only questions in the server block order`() {
        val blocks = listOf(SharingBlockType.HEADING, SharingBlockType.QUESTION, SharingBlockType.SCRIPTURE, SharingBlockType.QUESTION)
            .map { BulletinSharingBlock(it, it.name, null) }
        val numbered = numberedSharingBlocks(blocks)
        assertEquals(listOf(null, 1, null, 2), numbered.map { it.question })
        assertEquals(blocks, numbered.map { it.block })
    }

    @Test fun `losing history permission clears previously displayed member content`() = runTest(dispatcher) {
        val repository = FakeBulletinRepository()
        val vm = BulletinViewModel(repository)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.content)
        repository.historyResult = Result.failure(BulletinAccessDenied())
        vm.openHistory()
        advanceUntilIdle()
        assertNull(vm.uiState.value.content)
        assertTrue(vm.uiState.value.failed)
        assertTrue(vm.uiState.value.historyFailed)
    }

    @Test fun `cover date matches Figma and keeps leading zeros`() {
        assertEquals("10.11", bulletinDate(LocalDate(2026, 10, 11)))
        assertEquals("01.04", bulletinDate(LocalDate(2026, 1, 4)))
    }
}
