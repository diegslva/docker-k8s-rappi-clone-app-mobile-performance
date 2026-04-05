package com.rappiclone.geolocation.service

import com.rappiclone.domain.errors.DomainError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.geolocation.model.*
import com.rappiclone.geolocation.provider.GeocodingProvider
import com.rappiclone.geolocation.provider.RoutingProvider
import org.slf4j.LoggerFactory

class GeolocationService(
    private val geocodingProvider: GeocodingProvider,
    private val routingProvider: RoutingProvider
) {
    private val logger = LoggerFactory.getLogger(GeolocationService::class.java)

    suspend fun geocode(request: GeocodeRequest): List<GeocodeResult> {
        val query = buildString {
            append(request.address)
            if (!request.city.isNullOrBlank()) append(", ${request.city}")
            if (!request.state.isNullOrBlank()) append(", ${request.state}")
        }

        val results = geocodingProvider.geocode(query, request.country)
        logger.debug("Geocode '$query': ${results.size} resultados")
        return results
    }

    suspend fun reverseGeocode(request: ReverseGeocodeRequest): GeocodeResult {
        require(request.latitude in -90.0..90.0) { "Latitude invalida: ${request.latitude}" }
        require(request.longitude in -180.0..180.0) { "Longitude invalida: ${request.longitude}" }

        return geocodingProvider.reverseGeocode(request.latitude, request.longitude)
            ?: throw DomainException(DomainError.NotFound("Endereco", "(${request.latitude}, ${request.longitude})"))
    }

    suspend fun calculateRoute(request: RouteRequest): RouteResult {
        require(request.originLatitude in -90.0..90.0) { "originLatitude invalida" }
        require(request.originLongitude in -180.0..180.0) { "originLongitude invalida" }
        require(request.destinationLatitude in -90.0..90.0) { "destinationLatitude invalida" }
        require(request.destinationLongitude in -180.0..180.0) { "destinationLongitude invalida" }

        val result = routingProvider.route(
            request.originLatitude, request.originLongitude,
            request.destinationLatitude, request.destinationLongitude
        ) ?: throw DomainException(DomainError.InternalError("Nao foi possivel calcular rota"))

        logger.debug("Route: ${result.distanceKm}km, ${result.durationMinutes}min")
        return result
    }

    suspend fun calculateDistance(request: DistanceRequest): DistanceResult {
        val route = calculateRoute(RouteRequest(
            request.originLatitude, request.originLongitude,
            request.destinationLatitude, request.destinationLongitude
        ))
        return DistanceResult(
            distanceMeters = route.distanceMeters,
            durationSeconds = route.durationSeconds,
            distanceKm = route.distanceKm,
            durationMinutes = route.durationMinutes
        )
    }

    suspend fun validateAddress(request: ValidateAddressRequest): ValidateAddressResult {
        val query = "${request.street}, ${request.number}, ${request.neighborhood}, ${request.city}, ${request.state}, ${request.zipCode}"
        val results = geocodingProvider.geocode(query, request.country)

        if (results.isEmpty()) {
            return ValidateAddressResult(
                isValid = false,
                normalizedAddress = null,
                latitude = null,
                longitude = null,
                confidence = 0.0
            )
        }

        val best = results.first()
        val isValid = best.confidence >= 0.3

        return ValidateAddressResult(
            isValid = isValid,
            normalizedAddress = best.displayName,
            latitude = best.latitude,
            longitude = best.longitude,
            confidence = best.confidence
        )
    }
}
