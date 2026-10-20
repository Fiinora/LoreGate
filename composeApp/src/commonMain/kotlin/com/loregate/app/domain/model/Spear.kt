package com.loregate.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Spear(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val proximityRadius: Float = 50f,
    val description: String = "",
    val imageUrl: String = "",
    val difficulty: String = "EASY",
    val xpReward: Int = 25,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val locationName: String = "",
    val isCollected: Boolean = false,
    val collectedByUserId: String? = null,
    val collectedAt: Long? = null
) {
    @Transient
    val geoPoint: GeoPoint
        get() = GeoPoint(latitude = latitude, longitude = longitude)
}

@Serializable
data class SpearSnapshot(
    val totalCount: Int = 0,
    val availableSpears: List<Spear> = emptyList()
)
