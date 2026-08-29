package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The server renames these fields on the way out — the entity columns are
 * name/short_description/is_ministry_active, but MinistrySummaryDto publishes
 * them as title/subtitle/active. The Kotlin names stay aligned with the domain
 * model; @SerialName carries the wire names.
 */
@Serializable
data class MinistrySummaryResponse(
    val publicId: String,
    @SerialName("title") val name: String,
    @SerialName("subtitle") val shortDescription: String,
    val imageUrl: String? = null,
    val contacts: List<MinistryContactResponse> = emptyList(),
    @SerialName("active") val isActive: Boolean = true,
)

/** The server has no single leader field; the first contact plays that role. */
@Serializable
data class MinistryContactResponse(
    val name: String? = null,
    val role: String? = null,
)
