package com.loregate.app.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class HistoricalSite(
    val id: String = "",
    val nama: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Float = 100f,
    val type: SiteType = SiteType.MONUMENT,
    @get:PropertyName("isHidden") @set:PropertyName("isHidden")
    var isHidden: Boolean = false,
    @get:PropertyName("Location") @set:PropertyName("Location")
    var location: String = "",
    val storyGroup: String = "",
    val chapterOrder: Int = 1,
    val nextSiteId: String = ""
) {
    constructor() : this(
        id = "",
        nama = "",
        description = "",
        imageUrl = "",
        latitude = 0.0,
        longitude = 0.0,
        radius = 100f,
        type = SiteType.MONUMENT,
        isHidden = false,
        location = "",
        storyGroup = "",
        chapterOrder = 1,
        nextSiteId = ""
    )

    @get:Exclude
    val geoPoint: GeoPoint
        get() = GeoPoint(latitude = latitude, longitude = longitude)
}

enum class SiteType(val displayLabel: String, val emoji: String) {
    @PropertyName("TEMPLE")
    TEMPLE("Temple", "🛕"),

    @PropertyName("PALACE")
    PALACE("Palace", "🏰"),

    @PropertyName("MUSEUM")
    MUSEUM("Museum", "🏛️"),

    @PropertyName("MONUMENT")
    MONUMENT("Monument", "⛩️"),

    @PropertyName("BATTLEFIELD")
    BATTLEFIELD("Battlefield", "⚔️");

    companion object {
        fun fromString(value: String): SiteType {
            return when (value.uppercase()) {
                "TEMPLE" -> TEMPLE
                "PALACE" -> PALACE
                "MUSEUM" -> MUSEUM
                "MONUMENT" -> MONUMENT
                "BATTLEFIELD" -> BATTLEFIELD
                else -> MONUMENT
            }
        }
    }
}
