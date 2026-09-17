package com.hanmaum.dn.mobile.features.training.presentation.detail

import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplicationStatus

/*
 * The 신청 취소 dialog (#245), kept free of Compose so every rule here is unit-tested.
 * Figma: section `22 · 양육 신청 · Zustände`, board `신청 취소 · Dialog · Zustände`.
 */

/**
 * What the dialog asks and where the attempt stands.
 *
 * [status] decides the wording: a running 양육 is aborted (중단하기, and it will not count as
 * 수료), an open application is withdrawn (취소하기, the seat is freed).
 */
data class CancelPrompt(
    val status: TrainingApplicationStatus,
    val isCancelling: Boolean = false,
    val outcome: CancelOutcome? = null,
) {
    val isAbort: Boolean
        get() = status == TrainingApplicationStatus.IN_PROGRESS

    /**
     * Sending again cannot help once the server has given a reason — except after a plain
     * failure, where the application still stands and a retry is safe.
     */
    val canConfirm: Boolean
        get() = !isCancelling && (outcome == null || outcome.isRetryable)

    /** While the call runs the dialog stays put, so a stray tap outside cannot orphan it. */
    val isDismissable: Boolean
        get() = !isCancelling
}

/** Why the last attempt did not cancel. Success closes the dialog instead of filling this. */
sealed interface CancelOutcome {
    /** 409: already 수료 — an auskunft, not an error, and retrying cannot change it. */
    data object NotCancellable : CancelOutcome

    /** 404: nothing left to cancel. Closing reloads, so the page stops showing a stale state. */
    data object NotFound : CancelOutcome

    /** 503: application.hanmaum.de unreachable. Nothing was changed. */
    data object Unavailable : CancelOutcome

    /** Anything else. The application stands, so retrying is safe. */
    data object Failed : CancelOutcome
    ;

    /** Only a real failure is worth repeating; a refusal and 준비중 are not. */
    val isRetryable: Boolean
        get() = this is Failed
}
