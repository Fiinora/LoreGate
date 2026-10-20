package com.loregate.app.core.location

import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import kotlinx.coroutines.flow.Flow

data class UserLocationSample(
    val geoPoint: GeoPoint,
    val isMoving: Boolean
)

interface LocationTracker {
    fun observeLocationUpdates(): Flow<UserLocationSample>
}

interface GeofenceManager {
    fun registerSitesGeofences(sites: List<HistoricalSite>)
}
