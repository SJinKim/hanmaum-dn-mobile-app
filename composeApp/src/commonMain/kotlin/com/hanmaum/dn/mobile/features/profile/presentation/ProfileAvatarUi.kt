package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.HttpException
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.dnPressable
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnPillShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileAvatar
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure
import com.hanmaum.dn.mobile.features.profile.data.image.ProfileImageLoader

/**
 * The one avatar renderer, for the profile hub and the edit form. A
 * confirmed image is keyed `profile:{publicId}:{version}` in both caches —
 * never by the signed URL — and the unsaved preview never enters a cache.
 */
@Composable
internal fun ProfileAvatarImage(
    avatar: ProfileAvatar?,
    preview: PreparedProfileImage?,
    loader: ProfileImageLoader,
    size: Dp,
    onLoadFailed: (ProfileAvatar, Int?) -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val context = LocalPlatformContext.current
    Box(
        modifier
            .size(size)
            .clip(DnPillShape)
            .background(c.surface2)
            .border(1.5.dp, c.strokeStrong, DnPillShape)
            .semantics {
                contentDescription = if (avatar != null || preview != null) strings.profilePhoto else strings.profilePhotoNone
            },
        contentAlignment = Alignment.Center,
    ) {
        // Underneath, so it shows while loading, on a cache miss offline, and after a delete.
        Icon(DnIcons.User, null, tint = c.textPrimary, modifier = Modifier.size(size * 0.48f))
        val imageModifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.55f else 1f)
        when {
            preview != null -> AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(preview.preview)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                imageLoader = loader.imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
            avatar?.url != null -> AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatar.url)
                    .memoryCacheKey(avatar.cacheKey)
                    .diskCacheKey(avatar.cacheKey)
                    .build(),
                imageLoader = loader.imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
                onError = { error ->
                    onLoadFailed(avatar, (error.result.throwable as? HttpException)?.response?.code)
                },
            )
        }
    }
}

/** The photo block at the top of the edit form: avatar, its action, and what the photo is doing. */
@Composable
internal fun AvatarEditor(
    state: ProfileUiState.Success,
    loader: ProfileImageLoader,
    onChangePhoto: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onChooseOther: () -> Unit,
    onRecheck: () -> Unit,
    onRetryConnection: () -> Unit,
    onLoadFailed: (ProfileAvatar, Int?) -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val activity = state.avatarActivity

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProfileAvatarImage(
                avatar = state.avatar,
                preview = state.avatarPreview,
                loader = loader,
                size = 88.dp,
                onLoadFailed = onLoadFailed,
                dimmed = state.avatarPreview != null && activity !is AvatarActivity.Idle,
            )
            when (activity) {
                is AvatarActivity.Uploading -> {
                    val fraction = activity.fraction
                    if (fraction != null) {
                        CircularProgressIndicator(
                            progress = { fraction },
                            color = c.lime,
                            trackColor = c.surface3,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(96.dp),
                        )
                    } else {
                        CircularProgressIndicator(color = c.lime, strokeWidth = 3.dp, modifier = Modifier.size(96.dp))
                    }
                }
                AvatarActivity.Preparing, AvatarActivity.AwaitingServer,
                AvatarActivity.Deleting, AvatarActivity.Reconciling ->
                    CircularProgressIndicator(color = c.lime, strokeWidth = 3.dp, modifier = Modifier.size(96.dp))
                else -> Unit
            }
        }

        if (state.avatarPreview != null) {
            Text(strings.profilePhotoUnsaved.uppercase(), style = DnTheme.typography.label, color = c.amber)
        }

        when {
            activity.isBusy && activity !is AvatarActivity.Deleting && activity !is AvatarActivity.Reconciling ->
                AvatarPill(DnIcons.X, strings.cancel, c.textSecondary, onCancel)
            else -> AvatarPill(
                icon = DnIcons.Image,
                label = strings.profileChangePhoto,
                ink = c.textSecondary,
                onClick = onChangePhoto,
                enabled = state.canChangeAvatar,
            )
        }

        AvatarStatus(
            state,
            onRetry = onRetry,
            onChooseOther = onChooseOther,
            onRecheck = onRecheck,
            onRetryConnection = onRetryConnection,
        )

        Text(
            strings.profilePhotoSavesItself,
            style = DnTheme.typography.caption,
            color = c.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AvatarPill(
    icon: ImageVector,
    label: String,
    ink: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val c = DnTheme.colors
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .dnPressable(enabled = enabled, shape = DnPillShape, onClick = onClick)
            .background(c.surface2, DnPillShape)
            .border(1.dp, c.strokeSubtle, DnPillShape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(15.dp))
        Text(label, style = DnTheme.typography.captionStrong, color = ink)
    }
}

/** Progress is announced politely; errors and the open outcome sit in a card with their way out. */
@Composable
private fun AvatarStatus(
    state: ProfileUiState.Success,
    onRetry: () -> Unit,
    onChooseOther: () -> Unit,
    onRecheck: () -> Unit,
    onRetryConnection: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val progress = when (val activity = state.avatarActivity) {
        AvatarActivity.Preparing -> strings.profilePhotoPreparing
        is AvatarActivity.Uploading -> activity.fraction
            ?.let { strings.profilePhotoUploading((it * 100).toInt()) }
            ?: strings.profilePhotoUploadingIndeterminate
        AvatarActivity.AwaitingServer -> strings.profilePhotoProcessing
        AvatarActivity.Deleting -> strings.profilePhotoDeleting
        AvatarActivity.Reconciling -> strings.profilePhotoChecking
        else -> null
    }
    if (progress != null) {
        Text(
            progress,
            style = DnTheme.typography.caption,
            color = c.textSecondary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        return
    }

    when (val activity = state.avatarActivity) {
        is AvatarActivity.Failed -> NoticeCard(
            text = strings.avatarFailureMessage(activity.failure),
            container = c.redDim,
            ink = c.red,
            icon = DnIcons.AlertTriangle,
            action = when {
                activity.canRetry -> strings.retry to onRetry
                activity.failure.needsAnotherPhoto() -> strings.profilePhotoChooseOther to onChooseOther
                else -> null
            },
        )
        AvatarActivity.Uncertain -> NoticeCard(
            text = strings.profilePhotoUncertain,
            container = c.blueDim,
            ink = c.blue,
            icon = DnIcons.Clock,
            action = strings.profilePhotoRecheck to onRecheck,
        )
        else -> if (state.isOffline) {
            NoticeCard(
                text = strings.profilePhotoOffline,
                container = c.surface2,
                ink = c.textSecondary,
                icon = DnIcons.AlertTriangle,
                action = strings.retry to onRetryConnection,
            )
        }
    }
}

@Composable
private fun NoticeCard(
    text: String,
    container: Color,
    ink: Color,
    icon: ImageVector,
    action: Pair<String, () -> Unit>?,
) {
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(container, DnTileShape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(text, style = DnTheme.typography.caption, color = c.textPrimary, modifier = Modifier.weight(1f))
        }
        action?.let { (label, onClick) ->
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .dnPressable(shape = DnPillShape, onClick = onClick)
                    .background(c.surface, DnPillShape)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = DnTheme.typography.captionStrong, color = ink)
            }
        }
    }
}

/** 사진 선택 / 사진 삭제 / 취소 — only offered when there is a photo to remove. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarActionSheet(
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.profilePhoto, style = DnTheme.typography.title, color = c.textPrimary)
            Spacer(Modifier.height(8.dp))
            SheetAction(DnIcons.Image, strings.profilePhotoChoose, c.textPrimary, onChoose)
            SheetAction(DnIcons.Trash, strings.profilePhotoRemove, c.red, onRemove)
            SheetAction(DnIcons.X, strings.cancel, c.textSecondary, onDismiss)
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, ink: Color, onClick: () -> Unit) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .dnPressable(shape = DnTileShape, onClick = onClick)
            .background(c.surface2, DnTileShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp))
        Text(label, style = DnTheme.typography.captionStrong, color = ink)
    }
}

@Composable
internal fun AvatarDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(44.dp).clip(DnTileShape).background(c.redDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.Trash, null, tint = c.red, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                strings.profilePhotoDeleteTitle,
                style = DnTheme.typography.headline,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                strings.profilePhotoDeleteBody,
                style = DnTheme.typography.caption,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogAction(strings.cancel, c.surface2, c.textPrimary, onDismiss, Modifier.weight(1f))
                DialogAction(strings.profilePhotoDeleteConfirm, c.red, c.onRed, onConfirm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DialogAction(label: String, container: Color, ink: Color, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .dnPressable(shape = DnPillShape, onClick = onClick)
            .background(container, DnPillShape)
            .padding(vertical = 14.dp)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = DnTheme.typography.bodyStrong, color = ink)
    }
}

private fun ProfileImageFailure.needsAnotherPhoto(): Boolean = when (this) {
    ProfileImageFailure.TOO_LARGE, ProfileImageFailure.UNSUPPORTED_TYPE,
    ProfileImageFailure.INVALID_IMAGE, ProfileImageFailure.UNREADABLE -> true
    else -> false
}

internal fun AppStrings.avatarFailureMessage(failure: ProfileImageFailure): String = when (failure) {
    ProfileImageFailure.TOO_LARGE -> profilePhotoErrorTooLarge
    ProfileImageFailure.UNSUPPORTED_TYPE -> profilePhotoErrorUnsupported
    ProfileImageFailure.INVALID_IMAGE -> profilePhotoErrorInvalid
    ProfileImageFailure.STORAGE_UNAVAILABLE -> profilePhotoErrorStorage
    ProfileImageFailure.AUTHENTICATION_REQUIRED -> profilePhotoErrorAuth
    ProfileImageFailure.ACCESS_DENIED -> profilePhotoErrorDenied
    ProfileImageFailure.PROFILE_UNAVAILABLE -> profilePhotoErrorUnavailable
    ProfileImageFailure.NETWORK -> profilePhotoErrorNetwork
    ProfileImageFailure.UNREADABLE -> profilePhotoErrorUnreadable
    ProfileImageFailure.UNKNOWN -> profilePhotoErrorUnknown
}
