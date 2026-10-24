package com.hanmaum.dn.mobile.features.consent.domain.model

/** Typed outcome of a consent call, keeping Ktor and HTTP status codes out of the ViewModels. */
sealed interface ConsentResult<out T> {
    data class Success<T>(val data: T) : ConsentResult<T>

    /**
     * The version sent is no longer in force (409 CONSENT_VERSION_OUTDATED).
     *
     * The text changed while the screen was open. The caller reloads the policy and asks again,
     * with the box unticked; resending with the new version silently would record a text the
     * member never saw.
     */
    data object VersionOutdated : ConsentResult<Nothing>

    /** The server refused because the required consent is missing (CONSENT_REQUIRED). */
    data object ConsentRequired : ConsentResult<Nothing>

    data object Failed : ConsentResult<Nothing>
}
