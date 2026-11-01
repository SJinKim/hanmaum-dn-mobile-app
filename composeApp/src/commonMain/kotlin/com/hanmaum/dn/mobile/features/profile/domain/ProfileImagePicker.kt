package com.hanmaum.dn.mobile.features.profile.domain

/**
 * An image the member picked, still in the platform's hands — an Android
 * content URI or a private copy of the iOS provider file. Shared code never
 * looks inside; it only hands it to the [ProfileImagePreparer] and releases it.
 */
interface PickedProfileImage {
    fun release()
}

sealed interface ProfileImagePick {
    data class Selected(val image: PickedProfileImage) : ProfileImagePick
    data object Cancelled : ProfileImagePick
    /** The picker returned something that could not be read (e.g. an iCloud download failed). */
    data object Unreadable : ProfileImagePick
}

/**
 * The system photo picker: Android Photo Picker, iOS PHPicker. Images only,
 * one at a time, and no photo, camera or storage permission.
 */
interface ProfileImagePicker {
    suspend fun pick(): ProfileImagePick
}
