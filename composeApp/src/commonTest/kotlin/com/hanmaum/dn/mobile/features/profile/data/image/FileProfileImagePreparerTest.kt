package com.hanmaum.dn.mobile.features.profile.data.image

import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure
import com.hanmaum.dn.mobile.features.profile.domain.PROFILE_IMAGE_EDGE
import com.hanmaum.dn.mobile.features.profile.domain.PROFILE_IMAGE_MAX_BYTES
import com.hanmaum.dn.mobile.features.profile.domain.PickedProfileImage
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlinx.io.write
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class Picked : PickedProfileImage {
    override fun release() = Unit
}

private class Square : SquareImage {
    var recycled = false
    override fun recycle() { recycled = true }
}

/** Writes [sizes] bytes per attempt, one entry per quality step. */
private class FakeCodec(
    private val square: Square? = Square(),
    private val sizes: List<Long> = listOf(10_000),
) : ProfileImageCodec {
    val qualities = mutableListOf<Int>()
    var edge = 0
    override suspend fun decodeSquare(picked: PickedProfileImage, edge: Int): SquareImage? {
        this.edge = edge
        return square
    }
    override fun writeJpeg(image: SquareImage, quality: Int, path: String): Boolean {
        val size = sizes.getOrElse(qualities.size) { sizes.last() }
        qualities += quality
        SystemFileSystem.sink(Path(path)).buffered().use { it.write(ByteArray(size.toInt())) }
        return true
    }
}

class FileProfileImagePreparerTest {
    private val dir = Path(SystemTemporaryDirectory, "profile-image-test-${Random.nextLong().toULong()}")

    @AfterTest
    fun cleanUp() {
        if (SystemFileSystem.exists(dir)) {
            SystemFileSystem.list(dir).forEach { SystemFileSystem.delete(it) }
            SystemFileSystem.delete(dir)
        }
    }

    private fun files() = if (SystemFileSystem.exists(dir)) SystemFileSystem.list(dir) else emptyList()

    @Test
    fun preparesA512SquareJpegFileThatIsReleasable() = runTest {
        val square = Square()
        val codec = FakeCodec(square)
        val preparer = FileProfileImagePreparer(codec, dir.toString(), StandardTestDispatcher(testScheduler))

        val result = assertIs<ProfileImagePreparation.Ready>(preparer.prepare(Picked()))

        assertEquals(PROFILE_IMAGE_EDGE, codec.edge)
        assertEquals(listOf(85), codec.qualities)
        assertEquals(10_000L, result.image.byteLength)
        assertEquals(10_000, result.image.open().use { it.readByteArray().size })
        assertEquals(10_000, result.image.open().use { it.readByteArray().size }, "every open starts from the top")
        assertTrue(square.recycled)
        assertEquals(1, files().size)

        result.image.release()
        result.image.release()
        assertTrue(files().isEmpty())
    }

    @Test
    fun stepsQualityDownUntilUnderTheLimit() = runTest {
        val codec = FakeCodec(sizes = listOf(PROFILE_IMAGE_MAX_BYTES, PROFILE_IMAGE_MAX_BYTES - 1))
        val preparer = FileProfileImagePreparer(codec, dir.toString(), StandardTestDispatcher(testScheduler))

        val result = assertIs<ProfileImagePreparation.Ready>(preparer.prepare(Picked()))

        assertEquals(listOf(85, 75), codec.qualities)
        assertEquals(PROFILE_IMAGE_MAX_BYTES - 1, result.image.byteLength)
        result.image.release()
    }

    @Test
    fun tooLargeAtEveryQualityFailsAndLeavesNoFile() = runTest {
        val codec = FakeCodec(sizes = listOf(PROFILE_IMAGE_MAX_BYTES))
        val preparer = FileProfileImagePreparer(codec, dir.toString(), StandardTestDispatcher(testScheduler))

        val result = assertIs<ProfileImagePreparation.Failed>(preparer.prepare(Picked()))

        assertEquals(ProfileImageFailure.TOO_LARGE, result.failure)
        assertEquals(JPEG_QUALITY_LADDER, codec.qualities)
        assertTrue(files().isEmpty())
    }

    @Test
    fun anUndecodableImageIsInvalid() = runTest {
        val preparer = FileProfileImagePreparer(FakeCodec(square = null), dir.toString(), StandardTestDispatcher(testScheduler))

        val result = assertIs<ProfileImagePreparation.Failed>(preparer.prepare(Picked()))

        assertEquals(ProfileImageFailure.INVALID_IMAGE, result.failure)
    }

    @Test
    fun anEmptyOutputIsInvalid() = runTest {
        val preparer = FileProfileImagePreparer(FakeCodec(sizes = listOf(0)), dir.toString(), StandardTestDispatcher(testScheduler))

        val result = assertIs<ProfileImagePreparation.Failed>(preparer.prepare(Picked()))

        assertEquals(ProfileImageFailure.INVALID_IMAGE, result.failure)
        assertTrue(files().isEmpty())
    }

    @Test
    fun purgeRemovesLeftoversOnlyInItsOwnFolder() = runTest {
        SystemFileSystem.createDirectories(dir)
        val leftover = Path(dir, "avatar-old.jpg")
        SystemFileSystem.sink(leftover).buffered().use { it.write(ByteArray(3)) }
        val neighbour = Path(SystemTemporaryDirectory, "not-ours-${Random.nextLong().toULong()}.jpg")
        SystemFileSystem.sink(neighbour).buffered().use { it.write(ByteArray(3)) }

        FileProfileImagePreparer(FakeCodec(), dir.toString()).purgeStale()

        assertFalse(SystemFileSystem.exists(leftover))
        assertTrue(SystemFileSystem.exists(neighbour))
        SystemFileSystem.delete(neighbour)
    }

    @Test
    fun purgeWithoutAFolderIsHarmless() {
        FileProfileImagePreparer(FakeCodec(), dir.toString()).purgeStale()
    }
}
