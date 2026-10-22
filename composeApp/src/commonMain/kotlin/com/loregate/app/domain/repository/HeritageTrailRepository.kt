package com.loregate.app.domain.repository

import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HeritageTrail
import com.loregate.app.domain.model.HistoricalSite
import kotlinx.coroutines.flow.Flow

interface HeritageTrailRepository {
    fun observeHeritageTrails(): Flow<List<HeritageTrail>>
    suspend fun getHeritageTrails(): Result<List<HeritageTrail>>
    suspend fun getTrailById(trailId: String): Result<HeritageTrail?>
    suspend fun calculateTrailFullRoute(trail: HeritageTrail, allSites: List<HistoricalSite>): Result<List<GeoPoint>>
}
