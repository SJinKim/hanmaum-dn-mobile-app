package com.hanmaum.dn.mobile.features.training.domain.model

/**
 * What cancelling an application ended in (hanmaum-dn-server#177).
 *
 * One outcome per server error code, mirroring [ApplyResult]: the HTTP status alone decides
 * nothing, because 404 and 409 each carry two meanings the member has to be told apart.
 */
sealed interface CancelResult {
    /**
     * Cancelled, or already cancelled by an earlier call — the server answers both with 200.
     * [application] is the updated application (DROPPED); null only when it cannot be read,
     * in which case the caller reloads instead.
     */
    data class Success(val application: TrainingApplication?) : CancelResult

    /** 503: application.hanmaum.de could not be reached. Nothing was changed. */
    data object Unavailable : CancelResult

    /** 404: there is no application to cancel any more — cancelled elsewhere or removed by the office. */
    data object NotFound : CancelResult

    /** 409: the participation behind this application is already 수료, so it stays as it is. */
    data object NotCancellable : CancelResult

    data object Failed : CancelResult
}
