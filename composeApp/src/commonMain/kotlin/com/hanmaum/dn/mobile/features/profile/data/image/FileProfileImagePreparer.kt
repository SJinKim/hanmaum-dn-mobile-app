package com.hanmaum.dn.mobile.features.profile.data.image

import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure
import com.hanmaum.dn.mobile.features.profile.domain.PROFILE_IMAGE_EDGE
import com.hanmaum.dn.mobile.features.profile.domain.PROFILE_IMAGE_MAX_BYTES
import com.hanmaum.dn.mobile.features.profile.domain.PickedProfileImage
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparation
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.random.Random

/** A decoded, upright, square bitmap owned by the platform codec. */
interface SquareImage {
    fun recycle()
}

/**
 * The platform half of image preparation. Implementations decode with a
 * bounded resolution (never a full-size decode of a large original), apply
 * all eight EXIF orientations, crop the centre square, scale it to [edge],
 * and paint transparency on white.
 */
interface ProfileImageCodec {
    /** null when the image cannot be decoded. */
    suspend fun decodeSquare(picked: PickedProfileImage, edge: Int): SquareImage?

    /** Writes a baseline JPEG without any copied metadata. false on failure. */
    fun writeJpeg(image: SquareImage, quality: Int, path: String): Boolean
}

/** Qualities tried in order until the file is under the limit. */
internal val JPEG_QUALITY_LADDER = listOf(85, 75, 60)

/**
 * The shared half: file lifecycle, the size limit and typed failures. Files
 * live only in [directory], a private cache folder that is not backed up.
 */
class FileProfileImagePreparer(
    private val codec: ProfileImageCodec,
    directory: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ProfileImagePreparer {

    private val dir = Path(directory)

    override suspend fun prepare(picked: PickedProfileImage): ProfileImagePreparation = withContext(dispatcher) {
        val square = try {
            codec.decodeSquare(picked, PROFILE_IMAGE_EDGE)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return@withContext ProfileImagePreparation.Failed(ProfileImageFailure.INVALID_IMAGE)

        val path = Path(dir, "avatar-${Random.nextLong().toULong().toString(16)}.jpg")
        try {
            SystemFileSystem.createDirectories(dir)
            for (quality in JPEG_QUALITY_LADDER) {
                if (!codec.writeJpeg(square, quality, path.toString())) break
                val size = SystemFileSystem.metadataOrNull(path)?.size ?: 0L
                if (size in 1 until PROFILE_IMAGE_MAX_BYTES) {
                    return@withContext ProfileImagePreparation.Ready(FilePreparedProfileImage(path, size))
                }
                if (size <= 0L) break
                if (quality == JPEG_QUALITY_LADDER.last()) {
                    delete(path)
                    return@withContext ProfileImagePreparation.Failed(ProfileImageFailure.TOO_LARGE)
                }
            }
            delete(path)
            ProfileImagePreparation.Failed(ProfileImageFailure.INVALID_IMAGE)
        } catch (e: CancellationException) {
            delete(path)
            throw e
        } catch (_: Exception) {
            delete(path)
            ProfileImagePreparation.Failed(ProfileImageFailure.INVALID_IMAGE)
        } finally {
            square.recycle()
        }
    }

    override fun purgeStale() {
        runCatching {
            if (!SystemFileSystem.exists(dir)) return
            SystemFileSystem.list(dir).forEach { delete(it) }
        }
    }
}

private fun delete(path: Path) {
    runCatching { SystemFileSystem.delete(path, mustExist = false) }
}

/** A prepared JPEG on disk. [preview] is a plain file path, which Coil loads as a file. */
internal class FilePreparedProfileImage(
    private val path: Path,
    override val byteLength: Long,
) : PreparedProfileImage {
    override val preview: Any get() = path.toString()
    override fun open(): Source = SystemFileSystem.source(path).buffered()
    override fun release() = delete(path)
}
