package com.loregate.app.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
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
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    val locationName: String = "",
    val isCollected: Boolean = false,
    val collectedByUserId: String? = null,
    val collectedAt: Long? = null
) {
    constructor() : this(
        id = "",
        name = "",
        latitude = 0.0,
        longitude = 0.0,
        proximityRadius = 50f,
        description = "",
        imageUrl = "",
        difficulty = "EASY",
        xpReward = 25,
        isActive = true,
        createdAt = System.currentTimeMillis(),
        locationName = "",
        isCollected = false,
        collectedByUserId = null,
        collectedAt = null
    )

    @get:Exclude
    val geoPoint: GeoPoint
        get() = GeoPoint(latitude = latitude, longitude = longitude)
}

data class SpearSnapshot(
    val totalCount: Int = 0,
    val availableSpears: List<Spear> = emptyList()
)
