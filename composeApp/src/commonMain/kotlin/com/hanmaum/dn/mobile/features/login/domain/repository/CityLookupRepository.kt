package com.hanmaum.dn.mobile.features.login.domain.repository

/** Resolves a German postal code (PLZ) to its city/locality (Ort). */
interface CityLookupRepository {
    /**
     * Returns the city for [postalCode], or null when there is no match or the
     * lookup is unavailable (offline, server error). Callers fall back to manual
     * entry on null — a failed lookup never blocks registration.
     */
    suspend fun cityForPostalCode(postalCode: String): String?
}
