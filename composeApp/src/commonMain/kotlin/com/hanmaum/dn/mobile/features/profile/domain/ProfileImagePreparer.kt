package com.hanmaum.dn.mobile.features.profile.domain

import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage
import com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure

sealed interface ProfileImagePreparation {
    data class Ready(val image: PreparedProfileImage) : ProfileImagePreparation
    data class Failed(val failure: ProfileImageFailure) : ProfileImagePreparation
}

/**
 * Turns a picked image into the JPEG the server wants: upright, centre-cropped
 * square, [PROFILE_IMAGE_EDGE] pixels, no metadata, under [PROFILE_IMAGE_MAX_BYTES].
 */
interface ProfileImagePreparer {
    suspend fun prepare(picked: PickedProfileImage): ProfileImagePreparation

    /** Deletes files a killed process left in the private temp folder. Never touches other caches. */
    fun purgeStale()
}

const val PROFILE_IMAGE_EDGE = 512

/** The server's limit is exactly 5 MiB; the prepared file must stay below it. */
const val PROFILE_IMAGE_MAX_BYTES = 5L * 1024 * 1024
