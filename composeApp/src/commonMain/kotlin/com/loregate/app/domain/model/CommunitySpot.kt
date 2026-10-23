package com.loregate.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class CommunitySpotCategory(
    val displayLabel: String,
    val emoji: String,
    val hexColor: String
) {
    @SerialName("PHOTO_SPOT")
    PHOTO_SPOT("Spot Foto", "📸", "#EC4899"),

    @SerialName("HIDDEN_GEM")
    HIDDEN_GEM("Tempat Tersembunyi", "💎", "#8B5CF6"),

    @SerialName("FOLKLORE_STORY")
    FOLKLORE_STORY("Mitos & Cerita", "📜", "#F59E0B"),

    @SerialName("CULINARY_LOCAL")
    CULINARY_LOCAL("Kuliner Lokal", "🍜", "#EF4444"),

    @SerialName("TRAVEL_TIP")
    TRAVEL_TIP("Tips & Akses", "💡", "#10B981"),

    @SerialName("HERITAGE_DISCOVERY")
    HERITAGE_DISCOVERY("Situs Sejarah Baru", "🏛️", "#3B82F6");

    companion object {
        fun fromString(value: String): CommunitySpotCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HIDDEN_GEM
        }
    }
}

@Serializable
data class CommunitySpot(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: CommunitySpotCategory = CommunitySpotCategory.HIDDEN_GEM,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val targetSiteId: String? = null,
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val helpfulUserIds: List<String> = emptyList(),
    val reportCount: Int = 0,
    val reportedByUserIds: List<String> = emptyList(),
    val isVerifiedOnSite: Boolean = false,
    val isAutoHidden: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val geoPoint: GeoPoint
        get() = GeoPoint(latitude = latitude, longitude = longitude)

    val helpfulCount: Int
        get() = helpfulUserIds.size
}
