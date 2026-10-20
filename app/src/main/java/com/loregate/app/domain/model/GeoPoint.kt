package com.loregate.app.domain.model

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Calculates distance in meters between this [GeoPoint] and [other] using the Haversine formula.
     */
    fun distanceToMeters(other: GeoPoint): Float {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLng = Math.toRadians(other.longitude - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)

        val a = sin(dLat / 2).pow(2.0) +
            sin(dLng / 2).pow(2.0) * cos(lat1) * cos(lat2)
        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return (earthRadiusMeters * c).toFloat()
    }

    companion object {
        fun formatDistance(distanceInMeters: Float): String {
            return if (distanceInMeters < 1000f) {
                "${distanceInMeters.toInt()} m"
            } else {
                val distanceInKm = distanceInMeters / 1000f
                String.format("%.1f km", distanceInKm)
            }
        }

        fun generateCirclePolygon(
            center: GeoPoint,
            radiusMeters: Float,
            points: Int = 64
        ): List<GeoPoint> {
            val earthRadius = 6371000.0
            val latRad = Math.toRadians(center.latitude)
            val lngRad = Math.toRadians(center.longitude)
            val angularDistance = radiusMeters / earthRadius

            val coords = mutableListOf<GeoPoint>()
            for (i in 0..points) {
                val bearing = 2.0 * Math.PI * i / points
                val latPoint = asin(
                    sin(latRad) * cos(angularDistance) +
                        cos(latRad) * sin(angularDistance) * cos(bearing)
                )
                val lngPoint = lngRad + atan2(
                    sin(bearing) * sin(angularDistance) * cos(latRad),
                    cos(angularDistance) - sin(latRad) * sin(latPoint)
                )
                coords.add(
                    GeoPoint(
                        latitude = Math.toDegrees(latPoint),
                        longitude = Math.toDegrees(lngPoint)
                    )
                )
            }
            return coords
        }
    }
}
