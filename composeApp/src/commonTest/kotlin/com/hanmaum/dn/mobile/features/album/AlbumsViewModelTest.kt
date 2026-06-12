package com.hanmaum.dn.mobile.features.album

import com.hanmaum.dn.mobile.features.album.domain.model.Album
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumItem
import com.hanmaum.dn.mobile.features.album.domain.model.AlbumMeta
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumCacheRepository
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumDetailRepository
import com.hanmaum.dn.mobile.features.album.domain.repository.AlbumsRepository
import com.hanmaum.dn.mobile.features.album.presentation.albums.AlbumsUiState
import com.hanmaum.dn.mobile.features.album.presentation.albums.AlbumsViewModel
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
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest  fun tearDown() { Dispatchers.resetMain() }

    private val testAlbum = Album("uuid1", "여름수련회", "code1", 0)
    private val testItem = AlbumItem(1L, "cover.jpg", 1024L)

    private fun fakeAlbumsRepo(failure: Boolean = false) = object : AlbumsRepository {
        override suspend fun getAlbums() =
            if (failure) Result.failure(RuntimeException("네트워크 오류"))
            else Result.success(listOf(testAlbum))
    }

    private fun fakeDetailRepo(items: List<AlbumItem> = listOf(testItem)) = object : AlbumDetailRepository {
        override suspend fun getFolderContents(pcloudCode: String) = Result.success(items)
        override suspend fun getDownloadUrl(pcloudCode: String, fileId: Long) =
            Result.success("https://cdn.example.com/cover.jpg")
    }

    private fun emptyCache() = object : AlbumCacheRepository {
        private var list: List<Album>? = null
        private val meta = mutableMapOf<String, AlbumMeta>()
        override fun getCachedAlbumList() = list
        override fun saveAlbumList(albums: List<Album>) { list = albums }
        override fun getCachedMeta(pcloudCode: String) = meta[pcloudCode]
        override fun saveMeta(pcloudCode: String, m: AlbumMeta) { meta[pcloudCode] = m }
    }

    @Test
    fun `first launch resolves album cover and count`() = runTest {
        val vm = AlbumsViewModel(
            albumsRepository = fakeAlbumsRepo(),
            albumDetailRepository = fakeDetailRepo(),
            cacheRepository = emptyCache(),
        )
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        val state = assertIs<AlbumsUiState.Success>(vm.uiState.value)
        assertEquals(1, state.albums.size)
        assertEquals("여름수련회", state.albums[0].album.name)
        assertEquals("https://cdn.example.com/cover.jpg", state.albums[0].coverUrl)
        assertEquals(1, state.albums[0].photoCount)
    }

    @Test
    fun `cached cover url preserved when pCloud returns empty folder`() = runTest {
        val cache = emptyCache().also {
            it.saveAlbumList(listOf(testAlbum))
            it.saveMeta("code1", AlbumMeta("https://cached.example.com/cover.jpg", 5))
        }
        val vm = AlbumsViewModel(
            albumsRepository = fakeAlbumsRepo(),
            albumDetailRepository = fakeDetailRepo(emptyList()), // pCloud returns no photos
            cacheRepository = cache,
        )
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        val state = assertIs<AlbumsUiState.Success>(vm.uiState.value)
        assertEquals("https://cached.example.com/cover.jpg", state.albums[0].coverUrl)
        assertEquals(5, state.albums[0].photoCount)
    }

    @Test
    fun `backend failure with no cache emits Error`() = runTest {
        val vm = AlbumsViewModel(
            albumsRepository = fakeAlbumsRepo(failure = true),
            albumDetailRepository = fakeDetailRepo(),
            cacheRepository = emptyCache(),
        )
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<AlbumsUiState.Error>(vm.uiState.value)
    }
}
