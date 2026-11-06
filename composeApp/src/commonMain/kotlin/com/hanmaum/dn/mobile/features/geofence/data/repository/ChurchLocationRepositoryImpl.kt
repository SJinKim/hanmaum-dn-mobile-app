package com.hanmaum.dn.mobile.features.geofence.data.repository

import com.hanmaum.dn.mobile.core.network.legacyOrWrappedBody
import com.hanmaum.dn.mobile.features.geofence.data.model.ChurchLocationResponse
import com.hanmaum.dn.mobile.features.geofence.domain.model.ChurchLocation
import com.hanmaum.dn.mobile.features.geofence.domain.repository.ChurchLocationRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

class ChurchLocationRepositoryImpl(
    private val client: HttpClient,
) : ChurchLocationRepository {

    override suspend fun getChurchLocation(): Result<ChurchLocation> = try {
        val response = client.get("church/location")
        check(response.status == HttpStatusCode.OK) { "Unexpected status: ${response.status}" }
        Result.success(response.legacyOrWrappedBody<ChurchLocationResponse>().toDomain())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun ChurchLocationResponse.toDomain() = ChurchLocation(
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
    )
}
