package com.hanmaum.dn.mobile.features.training.domain.repository

import com.hanmaum.dn.mobile.features.training.domain.model.Training
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingResult

interface TrainingRepository {
    /** The 양육 list in the server's order: open for this member first, then by progression. */
    suspend fun getTrainings(): TrainingResult<List<Training>>

    suspend fun getTrainingDetail(publicId: String): TrainingResult<TrainingDetail>
}
