package com.hanmaum.dn.mobile.features.attendance

import com.hanmaum.dn.mobile.core.domain.repository.LocationPreferences
import com.hanmaum.dn.mobile.core.location.CurrentLocationProvider
import com.hanmaum.dn.mobile.core.location.LocationResult
import kotlinx.coroutines.awaitCancellation

class FakeCurrentLocationProvider(
    var result: LocationResult = LocationResult.PermissionDenied,
) : CurrentLocationProvider {
    var callCount = 0
    /** Never answers, the way a provider without a fix would hang. */
    var hangs = false

    override suspend fun getCurrentLocation(): LocationResult {
        callCount++
        if (hangs) awaitCancellation()
        return result
    }
}

class FakeLocationPreferences(
    private var sharingEnabled: Boolean = true,
) : LocationPreferences {
    override fun isSharingEnabled() = sharingEnabled
    override fun setSharingEnabled(value: Boolean) { sharingEnabled = value }
    override fun isPromptDismissed() = false
    override fun setPromptDismissed(value: Boolean) {}
}
