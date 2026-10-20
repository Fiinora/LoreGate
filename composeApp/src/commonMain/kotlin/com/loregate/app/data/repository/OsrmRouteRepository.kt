package com.loregate.app.data.repository

import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.repository.RouteRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OsrmRouteRepository(
    private val httpClient: HttpClient = HttpClient()
) : RouteRepository {

    companion object {
        private const val OSRM_API_URL = "https://router.project-osrm.org/route/v1/walking/"
    }

    override suspend fun getWalkingRoute(start: GeoPoint, end: GeoPoint): Result<List<GeoPoint>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val urlString =
                    "$OSRM_API_URL${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
                val response = httpClient.get(urlString).bodyAsText()
                parseOsrmResponse(response)
            }
        }

    private fun parseOsrmResponse(jsonString: String): List<GeoPoint> {
        val jsonElement = Json.parseToJsonElement(jsonString)
        val routes = jsonElement.jsonObject["routes"]?.jsonArray
        if (routes.isNullOrEmpty()) {
            throw NoSuchElementException("No walking route found.")
        }

        val firstRoute = routes[0].jsonObject
        val geometry = firstRoute["geometry"]?.jsonObject
        val coordinates = geometry?.get("coordinates")?.jsonArray
            ?: throw NoSuchElementException("No route coordinates found.")

        return coordinates.map { coordElement ->
            val pair = coordElement.jsonArray
            GeoPoint(
                longitude = pair[0].jsonPrimitive.double,
                latitude = pair[1].jsonPrimitive.double
            )
        }
    }
}
