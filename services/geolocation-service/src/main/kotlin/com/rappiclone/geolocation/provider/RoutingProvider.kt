package com.rappiclone.geolocation.provider

import com.rappiclone.geolocation.model.RouteResult

/**
 * Interface abstrata pra routing e ETA.
 * Implementacao concreta usa OSRM (self-hosted).
 * Permite trocar pra Valhalla ou outro engine sem impacto.
 */
interface RoutingProvider {
    suspend fun route(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): RouteResult?
}
