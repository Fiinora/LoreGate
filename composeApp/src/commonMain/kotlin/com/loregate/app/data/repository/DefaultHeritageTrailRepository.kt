package com.loregate.app.data.repository

import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HeritageTrail
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.repository.HeritageTrailRepository
import com.loregate.app.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DefaultHeritageTrailRepository(
    private val routeRepository: RouteRepository
) : HeritageTrailRepository {

    private val trails = HeritageTrail.buildDefaultTrails()

    override fun observeHeritageTrails(): Flow<List<HeritageTrail>> = flowOf(trails)

    override suspend fun getHeritageTrails(): Result<List<HeritageTrail>> = Result.success(trails)

    override suspend fun getTrailById(trailId: String): Result<HeritageTrail?> =
        Result.success(trails.find { it.id == trailId })

    override suspend fun calculateTrailFullRoute(
        trail: HeritageTrail,
        allSites: List<HistoricalSite>
    ): Result<List<GeoPoint>> = runCatching {
        val sitesMap = allSites.associateBy { it.id }
        val orderedSites = trail.siteIds.mapNotNull { sitesMap[it] }

        if (orderedSites.size < 2) {
            return@runCatching orderedSites.map { it.geoPoint }
        }

        val fullRoutePoints = mutableListOf<GeoPoint>()

        for (i in 0 until orderedSites.size - 1) {
            val fromSite = orderedSites[i]
            val toSite = orderedSites[i + 1]

            val segmentResult = routeRepository.getWalkingRoute(fromSite.geoPoint, toSite.geoPoint)
            val segmentPoints = segmentResult.getOrNull()

            if (!segmentPoints.isNullOrEmpty()) {
                if (fullRoutePoints.isNotEmpty() && segmentPoints.first() == fullRoutePoints.last()) {
                    fullRoutePoints.addAll(segmentPoints.drop(1))
                } else {
                    fullRoutePoints.addAll(segmentPoints)
                }
            } else {
                // Fallback direct line between sites
                if (fullRoutePoints.isEmpty()) {
                    fullRoutePoints.add(fromSite.geoPoint)
                }
                fullRoutePoints.add(toSite.geoPoint)
            }
        }

        fullRoutePoints
    }
}
