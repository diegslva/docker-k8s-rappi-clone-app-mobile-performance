package com.rappiclone.geolocation.routes

import com.rappiclone.geolocation.model.*
import com.rappiclone.geolocation.service.GeolocationService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.geolocationRoutes(geolocationService: GeolocationService) {
    route("/api/v1/geo") {

        post("/geocode") {
            val request = call.receive<GeocodeRequest>()
            val results = geolocationService.geocode(request)
            call.respond(HttpStatusCode.OK, results)
        }

        post("/reverse") {
            val request = call.receive<ReverseGeocodeRequest>()
            val result = geolocationService.reverseGeocode(request)
            call.respond(HttpStatusCode.OK, result)
        }

        post("/route") {
            val request = call.receive<RouteRequest>()
            val result = geolocationService.calculateRoute(request)
            call.respond(HttpStatusCode.OK, result)
        }

        post("/distance") {
            val request = call.receive<DistanceRequest>()
            val result = geolocationService.calculateDistance(request)
            call.respond(HttpStatusCode.OK, result)
        }

        post("/validate-address") {
            val request = call.receive<ValidateAddressRequest>()
            val result = geolocationService.validateAddress(request)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
