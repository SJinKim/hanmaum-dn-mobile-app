package com.hanmaum.dn.mobile.core.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

// Home (braucht das Token eigentlich nicht als Argument, speichern es global/secure, aber zur Practice wird es hier übergeben)
@Serializable
data class HomeRoute(val token: String)

// Announcement Detail
@Serializable
data class AnnouncementDetailRoute(val id: String, val token: String)

@Serializable
data class AnnouncementListRoute(val token: String)

@Serializable
object RegisterRoute