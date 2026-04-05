package com.rappiclone.domain.values

import kotlinx.serialization.Serializable
import kotlin.math.*

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude deve estar entre -90 e 90: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude deve estar entre -180 e 180: $longitude" }
    }

    /**
     * Calcula distancia em metros usando formula de Haversine.
     */
    fun distanceTo(other: Location): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLng = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
            sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }

    /**
     * Distancia em km, arredondada pra 2 casas.
     */
    fun distanceKmTo(other: Location): Double =
        (distanceTo(other) / 1000.0 * 100).roundToInt() / 100.0

    private fun Double.roundToInt(): Int = kotlin.math.roundToInt(this)
    private fun roundToInt(value: Double): Int = value.toInt()
}
