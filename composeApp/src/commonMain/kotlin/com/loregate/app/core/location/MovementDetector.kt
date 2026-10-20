package com.loregate.app.core.location

import com.loregate.app.domain.model.GeoPoint
import kotlin.math.abs

data class LocationRecord(
    val geoPoint: GeoPoint,
    val timeMillis: Long,
    val accuracy: Float = 0f
)

class MovementDetector {
    private var previousRecord: LocationRecord? = null
    private val locationHistory = mutableListOf<LocationRecord>()
    private val maxHistorySize = 5

    private val movementThreshold = 2.0f
    private val minTimeDelta = 1000L
    private val walkingSpeedThreshold = 0.5f
    private val minAccuracy = 20f

    fun isMoving(
        geoPoint: GeoPoint,
        accuracy: Float = 0f,
        timeMillis: Long = 0L
    ): Boolean {
        if (accuracy > 0 && accuracy > minAccuracy) {
            return false
        }

        val currentRecord = LocationRecord(geoPoint, timeMillis, accuracy)
        locationHistory.add(currentRecord)
        if (locationHistory.size > maxHistorySize) {
            locationHistory.removeAt(0)
        }

        val prev = previousRecord
        if (prev == null) {
            previousRecord = currentRecord
            return false
        }

        val timeDelta = timeMillis - prev.timeMillis
        if (timeDelta < minTimeDelta) {
            return false
        }

        val distance = prev.geoPoint.distanceToMeters(geoPoint)
        val speed = if (timeDelta > 0) distance / (timeDelta / 1000f) else 0f
        val avgSpeed = calculateAverageSpeed()

        previousRecord = currentRecord

        val hasMovedDistance = distance > movementThreshold
        val hasWalkingSpeed = speed > walkingSpeedThreshold || avgSpeed > walkingSpeedThreshold

        return hasMovedDistance && hasWalkingSpeed
    }

    private fun calculateAverageSpeed(): Float {
        if (locationHistory.size < 2) return 0f

        var totalDistance = 0f
        var totalTime = 0L

        for (i in 1 until locationHistory.size) {
            val r1 = locationHistory[i - 1]
            val r2 = locationHistory[i]
            totalDistance += r1.geoPoint.distanceToMeters(r2.geoPoint)
            totalTime += abs(r2.timeMillis - r1.timeMillis)
        }

        return if (totalTime > 0) totalDistance / (totalTime / 1000f) else 0f
    }

    fun reset() {
        previousRecord = null
        locationHistory.clear()
    }
}
