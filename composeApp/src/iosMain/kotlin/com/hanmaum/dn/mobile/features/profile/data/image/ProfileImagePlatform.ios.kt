@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hanmaum.dn.mobile.features.profile.data.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hanmaum.dn.mobile.features.profile.domain.PickedProfileImage
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePick
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePicker
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

private const val IMAGE_TYPE = "public.image"

/** Where picked originals are copied before preparation; purged with the prepared files. */
internal fun profileImageTempDirectory(): String = NSTemporaryDirectory().trimEnd('/') + "/profile-image"

/** A private copy of the provider file. The provider deletes its own file once the callback returns. */
internal class IosPickedProfileImage(val path: String) : PickedProfileImage {
    override fun release() {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}

private class PickerDelegate(
    private val onFinish: (PHPickerResult?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        onFinish(didFinishPicking.firstOrNull() as? PHPickerResult)
    }
}

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }
    var top = window?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

private class IosProfileImagePicker : ProfileImagePicker {
    // PHPickerViewController holds its delegate weakly.
    private var delegate: PickerDelegate? = null

    override suspend fun pick(): ProfileImagePick = suspendCancellableCoroutine { continuation ->
        if (delegate != null) {
            continuation.resume(ProfileImagePick.Cancelled)
            return@suspendCancellableCoroutine
        }
        val presenter = topViewController()
        if (presenter == null) {
            continuation.resume(ProfileImagePick.Unreadable)
            return@suspendCancellableCoroutine
        }
        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val controller = PHPickerViewController(configuration)
        delegate = PickerDelegate { result ->
            delegate = null
            val provider = result?.itemProvider
            if (provider == null) {
                if (continuation.isActive) continuation.resume(ProfileImagePick.Cancelled)
                return@PickerDelegate
            }
            provider.loadFileRepresentationForTypeIdentifier(IMAGE_TYPE) { url, _ ->
                // Copy inside the callback: the system removes `url` as soon as it returns.
                val copy = url?.let(::copyToPrivateTemp)
                if (continuation.isActive) {
                    continuation.resume(copy?.let { ProfileImagePick.Selected(IosPickedProfileImage(it)) } ?: ProfileImagePick.Unreadable)
                } else {
                    // A late callback after the member left: still free the copy.
                    copy?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
                }
            }
        }
        controller.delegate = delegate
        continuation.invokeOnCancellation {
            dispatch_async(dispatch_get_main_queue()) {
                if (controller.presentingViewController != null) controller.dismissViewControllerAnimated(true, null)
                delegate = null
            }
        }
        presenter.presentViewController(controller, animated = true, completion = null)
    }
}

private fun copyToPrivateTemp(source: NSURL): String? {
    val manager = NSFileManager.defaultManager
    val dir = profileImageTempDirectory()
    manager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    val target = "$dir/picked-${NSUUID().UUIDString}"
    val ok = manager.copyItemAtURL(source, toURL = NSURL.fileURLWithPath(target), error = null)
    return target.takeIf { ok }
}

@Composable
actual fun rememberProfileImagePicker(): ProfileImagePicker = remember { IosProfileImagePicker() }
