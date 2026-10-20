package com.loregate.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class HistoricalSite(
    val id: String = "",
    val nama: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Float = 100f,
    val type: SiteType = SiteType.MONUMENT,
    @SerialName("isHidden")
    var isHidden: Boolean = false,
    @SerialName("Location")
    var location: String = "",
    val storyGroup: String = "",
    val chapterOrder: Int = 1,
    val nextSiteId: String = ""
) {
    @Transient
    val geoPoint: GeoPoint
        get() = GeoPoint(latitude = latitude, longitude = longitude)
}

@Serializable
enum class SiteType(val displayLabel: String, val emoji: String) {
    @SerialName("TEMPLE")
    TEMPLE("Temple", "🛕"),

    @SerialName("PALACE")
    PALACE("Palace", "🏰"),

    @SerialName("MUSEUM")
    MUSEUM("Museum", "🏛️"),

    @SerialName("MONUMENT")
    MONUMENT("Monument", "⛩️"),

    @SerialName("BATTLEFIELD")
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
