package com.rappiclone.geolocation.provider

import com.rappiclone.geolocation.model.GeocodeResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Geocoding via Nominatim (OpenStreetMap, self-hosted).
 * API: https://nominatim.org/release-docs/develop/api/
 */
class NominatimProvider(
    private val baseUrl: String,
    private val httpClient: HttpClient
) : GeocodingProvider {

    private val logger = LoggerFactory.getLogger(NominatimProvider::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun geocode(query: String, country: String): List<GeocodeResult> {
        val response: HttpResponse = httpClient.get("$baseUrl/search") {
            parameter("q", query)
            parameter("format", "jsonv2")
            parameter("addressdetails", "1")
            parameter("countrycodes", country.lowercase())
            parameter("limit", "5")
        }

        val body = response.bodyAsText()
        val results = json.decodeFromString<List<NominatimSearchResult>>(body)

        return results.map { it.toGeocodeResult() }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): GeocodeResult? {
        val response: HttpResponse = httpClient.get("$baseUrl/reverse") {
            parameter("lat", latitude)
            parameter("lon", longitude)
            parameter("format", "jsonv2")
            parameter("addressdetails", "1")
        }

        val body = response.bodyAsText()
        return try {
            val result = json.decodeFromString<NominatimReverseResult>(body)
            result.toGeocodeResult()
        } catch (e: Exception) {
            logger.warn("Reverse geocode falhou pra ($latitude, $longitude): ${e.message}")
            null
        }
    }
}

@Serializable
internal data class NominatimSearchResult(
    val lat: String,
    val lon: String,
    val display_name: String = "",
    val importance: Double = 0.0,
    val address: NominatimAddress? = null
) {
    fun toGeocodeResult(): GeocodeResult = GeocodeResult(
        latitude = lat.toDouble(),
        longitude = lon.toDouble(),
        displayName = display_name,
        street = address?.road,
        number = address?.house_number,
        neighborhood = address?.suburb ?: address?.neighbourhood,
        city = address?.city ?: address?.town ?: address?.village,
        state = address?.state,
        zipCode = address?.postcode,
        country = address?.country_code?.uppercase(),
        confidence = importance.coerceIn(0.0, 1.0)
    )
}

@Serializable
internal data class NominatimReverseResult(
    val lat: String,
    val lon: String,
    val display_name: String = "",
    val importance: Double = 0.0,
    val address: NominatimAddress? = null
) {
    fun toGeocodeResult(): GeocodeResult = GeocodeResult(
        latitude = lat.toDouble(),
        longitude = lon.toDouble(),
        displayName = display_name,
        street = address?.road,
        number = address?.house_number,
        neighborhood = address?.suburb ?: address?.neighbourhood,
        city = address?.city ?: address?.town ?: address?.village,
        state = address?.state,
        zipCode = address?.postcode,
        country = address?.country_code?.uppercase(),
        confidence = importance.coerceIn(0.0, 1.0)
    )
}

@Serializable
internal data class NominatimAddress(
    val house_number: String? = null,
    val road: String? = null,
    val suburb: String? = null,
    val neighbourhood: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val country_code: String? = null
)
