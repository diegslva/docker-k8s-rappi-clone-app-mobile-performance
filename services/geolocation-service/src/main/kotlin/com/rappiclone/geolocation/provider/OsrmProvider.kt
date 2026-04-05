package com.rappiclone.geolocation.provider

import com.rappiclone.geolocation.model.RouteResult
import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Routing via OSRM (Open Source Routing Machine, self-hosted).
 * API: http://project-osrm.org/docs/v5.24.0/api/
 */
class OsrmProvider(
    private val baseUrl: String,
    private val httpClient: HttpClient
) : RoutingProvider {

    private val logger = LoggerFactory.getLogger(OsrmProvider::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun route(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): RouteResult? {
        // OSRM usa formato: /route/v1/driving/lng,lat;lng,lat
        val coordinates = "$originLng,$originLat;$destLng,$destLat"

        val response: HttpResponse = httpClient.get("$baseUrl/route/v1/driving/$coordinates") {
            parameter("overview", "simplified")
            parameter("geometries", "polyline")
            parameter("steps", "false")
        }

        val body = response.bodyAsText()
        return try {
            val osrmResponse = json.decodeFromString<OsrmRouteResponse>(body)
            val route = osrmResponse.routes.firstOrNull() ?: return null

            RouteResult(
                distanceMeters = route.distance,
                durationSeconds = route.duration,
                distanceKm = route.distance / 1000.0,
                durationMinutes = route.duration / 60.0,
                geometry = route.geometry
            )
        } catch (e: Exception) {
            logger.error("OSRM route failed ($originLat,$originLng -> $destLat,$destLng): ${e.message}")
            null
        }
    }
}

@Serializable
internal data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList()
)

@Serializable
internal data class OsrmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: String? = null
)
