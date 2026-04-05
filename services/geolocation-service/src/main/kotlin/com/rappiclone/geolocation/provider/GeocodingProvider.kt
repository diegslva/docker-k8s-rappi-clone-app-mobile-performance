package com.rappiclone.geolocation.provider

import com.rappiclone.geolocation.model.GeocodeResult

/**
 * Interface abstrata pra geocoding.
 * Implementacao concreta usa Nominatim (OSM, self-hosted).
 * Permite trocar pra outro provider sem impacto.
 */
interface GeocodingProvider {
    suspend fun geocode(query: String, country: String = "BR"): List<GeocodeResult>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): GeocodeResult?
}
