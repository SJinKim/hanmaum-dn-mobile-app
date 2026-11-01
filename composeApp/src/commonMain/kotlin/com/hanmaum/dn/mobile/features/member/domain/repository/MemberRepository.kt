package com.hanmaum.dn.mobile.features.member.domain.repository

import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import com.hanmaum.dn.mobile.features.member.domain.model.PreparedProfileImage

interface MemberRepository {
    suspend fun getMyProfile(): Result<MemberResponse>
    suspend fun updateMyProfile(
        phoneNumber: String?,
        birthDate: String?,
        street: String?,
        houseNumber: String?,
        zipCode: String?,
        city: String?,
        identityStatus: String?,
        workOrSchool: String?,
    ): Result<MemberResponse>

    /**
     * PUT members/me/profile-image as multipart part `file`. Fails with a
     * [com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageException];
     * [onProgress] reports bytes sent and the total when known. All bytes sent
     * is not success: only the returned profile is.
     */
    suspend fun uploadProfileImage(
        image: PreparedProfileImage,
        onProgress: (sent: Long, total: Long?) -> Unit,
    ): Result<MemberResponse>

    /** DELETE members/me/profile-image. Fails with a ProfileImageException. */
    suspend fun deleteProfileImage(): Result<MemberResponse>
}
