package com.hanmaum.dn.mobile.features.training.domain.repository

import com.hanmaum.dn.mobile.features.training.domain.model.ApplicationField
import com.hanmaum.dn.mobile.features.training.domain.model.ApplyResult
import com.hanmaum.dn.mobile.features.training.domain.model.Training
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult

interface TrainingRepository {
    /** The 양육 list in the server's order: open for this member first, then by progression. */
    suspend fun getTrainings(): TrainingResult<List<Training>>

    suspend fun getTrainingDetail(publicId: String): TrainingResult<TrainingDetail>

    /**
     * Applies the signed-in member to one course of a training.
     *
     * [values] holds only what the member filled in, with the birth date as YYYY-MM-DD.
     * Whatever is left out, the server takes from the profile. Safe to retry: the server
     * answers a repeat with the application already made.
     */
    suspend fun apply(
        trainingPublicId: String,
        externalCourseId: Int,
        values: Map<ApplicationField, String>,
    ): ApplyResult
}
