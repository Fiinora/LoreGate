package com.loregate.app.core.location

import android.location.Location
import javax.inject.Inject
import kotlin.math.abs

class MovementDetector @Inject constructor() {
    private var previousLocation: Location? = null
    private var previousTime: Long = 0L

    private val locationHistory = mutableListOf<Location>()
    private val maxHistorySize = 5

    private val movementThreshold = 2.0f
    private val minTimeDelta = 1000L
    private val walkingSpeedThreshold = 0.5f
    private val minAccuracy = 20f

    fun isMoving(currentLocation: Location): Boolean {
        val currentTime = System.currentTimeMillis()

        if (currentLocation.hasAccuracy() && currentLocation.accuracy > minAccuracy) {
            return false
        }

        locationHistory.add(currentLocation)
        if (locationHistory.size > maxHistorySize) {
            locationHistory.removeAt(0)
        }

        val prevLoc = previousLocation
        if (prevLoc == null) {
            previousLocation = currentLocation
            previousTime = currentTime
            return false
        }

        val timeDelta = currentTime - previousTime
        if (timeDelta < minTimeDelta) {
            return false
        }

        val distance = prevLoc.distanceTo(currentLocation)
        val speed = if (timeDelta > 0) distance / (timeDelta / 1000f) else 0f
        val avgSpeed = calculateAverageSpeed()

        previousLocation = currentLocation
        previousTime = currentTime

        val hasMovedDistance = distance > movementThreshold
        val hasWalkingSpeed = speed > walkingSpeedThreshold || avgSpeed > walkingSpeedThreshold

        return hasMovedDistance && hasWalkingSpeed
    }

    private fun calculateAverageSpeed(): Float {
        if (locationHistory.size < 2) return 0f

        var totalDistance = 0f
        var totalTime = 0L

        for (i in 1 until locationHistory.size) {
            val loc1 = locationHistory[i - 1]
            val loc2 = locationHistory[i]
            totalDistance += loc1.distanceTo(loc2)
            totalTime += abs(loc2.time - loc1.time)
        }

        return if (totalTime > 0) totalDistance / (totalTime / 1000f) else 0f
    }

    fun reset() {
        previousLocation = null
        previousTime = 0L
        locationHistory.clear()
    }
}
