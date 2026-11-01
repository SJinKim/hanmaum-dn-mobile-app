package com.hanmaum.dn.mobile.features.profile.data.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hanmaum.dn.mobile.features.profile.domain.PickedProfileImage
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePick
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePicker
import kotlinx.coroutines.CompletableDeferred

/**
 * The URI the Photo Picker granted for this pick. It is read once by the
 * codec and never persisted, so no long-lived permission is taken.
 */
internal class AndroidPickedProfileImage(val uri: Uri) : PickedProfileImage {
    // Nothing is copied on Android: the codec streams straight from the URI.
    override fun release() = Unit
}

/** Holds the one pick in flight; a result delivered to a recreated Activity finds none and is dropped. */
private class PendingPick {
    var deferred: CompletableDeferred<Uri?>? = null
}

@Composable
actual fun rememberProfileImagePicker(): ProfileImagePicker {
    val pending = remember { PendingPick() }
    // PickVisualMedia falls back to the document picker where the system Photo
    // Picker is missing — still without READ_MEDIA_IMAGES or storage access.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pending.deferred?.complete(uri)
    }
    return remember(launcher) {
        object : ProfileImagePicker {
            override suspend fun pick(): ProfileImagePick {
                if (pending.deferred != null) return ProfileImagePick.Cancelled
                val deferred = CompletableDeferred<Uri?>()
                pending.deferred = deferred
                return try {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    deferred.await()?.let { ProfileImagePick.Selected(AndroidPickedProfileImage(it)) }
                        ?: ProfileImagePick.Cancelled
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    ProfileImagePick.Unreadable
                } finally {
                    pending.deferred = null
                }
            }
        }
    }
}
