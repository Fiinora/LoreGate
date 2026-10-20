package com.loregate.app.data.repository

import android.util.Log
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.repository.RouteRepository
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class OsrmRouteRepository @Inject constructor() : RouteRepository {

    companion object {
        private const val OSRM_API_URL = "https://router.project-osrm.org/route/v1/walking/"
    }

    override suspend fun getWalkingRoute(start: GeoPoint, end: GeoPoint): Result<List<GeoPoint>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val urlString =
                    "$OSRM_API_URL${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
                val response = URL(urlString).readText()
                parseOsrmResponse(response)
            }.onFailure { e ->
                Log.e("OsrmRouteRepository", "Error fetching walking route", e)
            }
        }

    private fun parseOsrmResponse(jsonString: String): List<GeoPoint> {
        val json = JSONObject(jsonString)
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) {
            throw NoSuchElementException("No walking route found.")
        }

        val geometry = routes.getJSONObject(0).getJSONObject("geometry")
        val coordinates = geometry.getJSONArray("coordinates")

        val path = ArrayList<GeoPoint>(coordinates.length())
        for (i in 0 until coordinates.length()) {
            val coord = coordinates.getJSONArray(i)
            path.add(
                GeoPoint(
                    latitude = coord.getDouble(1),
                    longitude = coord.getDouble(0)
                )
            )
        }
        return path
    }
}
