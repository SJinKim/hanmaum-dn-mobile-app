package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The server renames some fields on the way out — the entity columns are
 * name/short_description, but MinistrySummaryDto publishes them as
 * title/subtitle. The Kotlin names stay aligned with the domain model;
 * @SerialName carries the wire names.
 *
 * isActive is NOT renamed. It once looked renamed because springdoc read the
 * Java getter and wrote "active" into openapi.yaml while Jackson serialized
 * "isActive" — so a client modelled from the spec never bound. The server
 * pinned all 19 is-prefixed booleans with an explicit @get:JsonProperty in
 * hanmaum-dn-server#138, and BooleanWireNameTest holds them there, so spec
 * and wire now agree.
 *
 * No default here on purpose: the default is what turned that mismatch into a
 * silent true instead of a failing test (#129).
 */
@Serializable
data class MinistrySummaryResponse(
    val publicId: String,
    @SerialName("title") val name: String,
    @SerialName("subtitle") val shortDescription: String,
    val imageUrl: String? = null,
    val contacts: List<MinistryContactResponse> = emptyList(),
    val isActive: Boolean,
)

/** The server has no single leader field; the first contact plays that role. */
@Serializable
data class MinistryContactResponse(
    val name: String? = null,
    val role: String? = null,
)
