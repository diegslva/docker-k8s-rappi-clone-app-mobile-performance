package com.rappiclone.profile.routes

import com.rappiclone.profile.model.*
import com.rappiclone.profile.service.ProfileService
import com.rappiclone.infra.tenant.tenantId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.profileRoutes(profileService: ProfileService) {
    route("/api/v1/users") {

        post("/profile") {
            val request = call.receive<CreateProfileRequest>()
            val tenant = call.tenantId
            val response = profileService.createProfile(request, tenant)
            call.respond(HttpStatusCode.Created, response)
        }

        get("/{userId}/profile") {
            val userId = call.parameters["userId"]!!
            val tenant = call.tenantId
            val response = profileService.getProfile(userId, tenant)
            call.respond(HttpStatusCode.OK, response)
        }

        put("/{userId}/profile") {
            val userId = call.parameters["userId"]!!
            val tenant = call.tenantId
            val request = call.receive<UpdateProfileRequest>()
            val response = profileService.updateProfile(userId, tenant, request)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/{userId}/addresses") {
            val userId = call.parameters["userId"]!!
            val tenant = call.tenantId
            val request = call.receive<CreateAddressRequest>()
            val response = profileService.addAddress(userId, tenant, request)
            call.respond(HttpStatusCode.Created, response)
        }

        delete("/{userId}/addresses/{addressId}") {
            val userId = call.parameters["userId"]!!
            val addressId = call.parameters["addressId"]!!
            val tenant = call.tenantId
            profileService.deleteAddress(userId, tenant, addressId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
