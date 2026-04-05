package com.rappiclone.tenant.routes

import com.rappiclone.tenant.model.*
import com.rappiclone.tenant.service.TenantService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tenantRoutes(tenantService: TenantService) {
    route("/api/v1/tenants") {

        post {
            val request = call.receive<CreateTenantRequest>()
            val response = tenantService.createTenant(request)
            call.respond(HttpStatusCode.Created, response)
        }

        get {
            val activeOnly = call.request.queryParameters["active"]?.toBoolean() ?: false
            val tenants = tenantService.listTenants(activeOnly)
            call.respond(HttpStatusCode.OK, tenants)
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            val tenant = tenantService.getTenant(id)
            call.respond(HttpStatusCode.OK, tenant)
        }

        put("/{id}") {
            val id = call.parameters["id"]!!
            val request = call.receive<UpdateTenantRequest>()
            val tenant = tenantService.updateTenant(id, request)
            call.respond(HttpStatusCode.OK, tenant)
        }

        // Zonas de um tenant
        post("/{tenantId}/zones") {
            val tenantId = call.parameters["tenantId"]!!
            val request = call.receive<CreateZoneRequest>()
            val zone = tenantService.createZone(tenantId, request)
            call.respond(HttpStatusCode.Created, zone)
        }

        get("/{tenantId}/zones") {
            val tenantId = call.parameters["tenantId"]!!
            val zones = tenantService.listZones(tenantId)
            call.respond(HttpStatusCode.OK, zones)
        }
    }

    // Endpoint de resolucao de localizacao (usado pelo API Gateway)
    route("/api/v1/location") {
        post("/resolve") {
            val request = call.receive<ResolveLocationRequest>()
            val resolved = tenantService.resolveLocation(request)
            call.respond(HttpStatusCode.OK, resolved)
        }
    }
}
