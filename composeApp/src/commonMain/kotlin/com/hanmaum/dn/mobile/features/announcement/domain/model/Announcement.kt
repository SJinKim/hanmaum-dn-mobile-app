package com.hanmaum.dn.mobile.features.announcement.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val startAt: String,
    val endAt: String?,
    val isPinned: Boolean,
    val category: String
) {
    fun getAnnouncementCategoryName(): String {
        return when(category) {
            "MINISTRY" -> "사역"
            "NOTICE"   -> "공지"
            "EVENT"    -> "행사"
            else       -> "알림" // Fallback für unbekannte Werte
        }
    }

    fun getAnnouncementCategoryColor(): Long {
        return when(category) {
            "MINISTRY" -> 0xFFE65100 // Orange
            "NOTICE"   -> 0xFFC62828 // Rot
            "EVENT"    -> 0xFF1565C0 // Blau
            else       -> 0xFF757575 // Grau
        }
    }
}

