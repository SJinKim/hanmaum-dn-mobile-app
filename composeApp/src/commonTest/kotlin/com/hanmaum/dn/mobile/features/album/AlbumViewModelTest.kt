package com.hanmaum.dn.mobile.features.album

import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumRepository
import com.hanmaum.dn.mobile.features.album.presentation.AlbumUiState
import com.hanmaum.dn.mobile.features.album.presentation.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest  fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `success loads items and resolves urls`() = runTest {
        val fakeRepo = object : AlbumRepository {
            override suspend fun getFolderContents() = Result.success(
                listOf(AlbumItem(1L, "photo.jpg", 102400L))
            )
            override suspend fun getDownloadUrl(fileId: Long) =
                Result.success("https://cdn.example.com/photo.jpg")
        }
        val vm = AlbumViewModel(fakeRepo)
        dispatcher.scheduler.advanceUntilIdle()

        val state = assertIs<AlbumUiState.Success>(vm.uiState.value)
        assertEquals(1, state.items.size)
        assertEquals("https://cdn.example.com/photo.jpg", state.resolvedUrls[1L])
    }

    @Test
    fun `failure from repo results in Error state`() = runTest {
        val fakeRepo = object : AlbumRepository {
            override suspend fun getFolderContents() =
                Result.failure<List<AlbumItem>>(RuntimeException("네트워크 오류"))
            override suspend fun getDownloadUrl(fileId: Long) = Result.success("")
        }
        val vm = AlbumViewModel(fakeRepo)
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<AlbumUiState.Error>(vm.uiState.value)
    }
}
