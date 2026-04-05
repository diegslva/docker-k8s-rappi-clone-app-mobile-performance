package com.rappiclone.onboarding.routes

import com.rappiclone.domain.enums.OnboardingStatus
import com.rappiclone.onboarding.model.*
import com.rappiclone.onboarding.service.OnboardingService
import com.rappiclone.infra.tenant.tenantId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.onboardingRoutes(onboardingService: OnboardingService) {

    // --- Store Applications ---
    route("/api/v1/onboarding/stores") {

        post {
            val request = call.receive<CreateStoreApplicationRequest>()
            val tenant = call.tenantId
            val ownerUserId = UUID.fromString(call.request.headers["X-User-ID"]
                ?: throw IllegalArgumentException("X-User-ID header obrigatorio"))

            val response = onboardingService.createStoreApplication(request, ownerUserId, tenant)
            call.respond(HttpStatusCode.Created, response)
        }

        get {
            val tenant = call.tenantId
            val status = call.request.queryParameters["status"]?.let { OnboardingStatus.valueOf(it.uppercase()) }
            val apps = onboardingService.listStoreApplications(tenant, status)
            call.respond(HttpStatusCode.OK, apps)
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            val app = onboardingService.getStoreApplication(id, tenant)
            call.respond(HttpStatusCode.OK, app)
        }

        post("/{id}/documents") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            val request = call.receive<AddDocumentRequest>()
            val doc = onboardingService.addStoreDocument(id, tenant, request)
            call.respond(HttpStatusCode.Created, doc)
        }

        post("/{id}/review") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            val reviewedBy = UUID.fromString(call.request.headers["X-User-ID"]
                ?: throw IllegalArgumentException("X-User-ID header obrigatorio"))
            val request = call.receive<ReviewApplicationRequest>()
            val app = onboardingService.reviewStoreApplication(id, tenant, reviewedBy, request)
            call.respond(HttpStatusCode.OK, app)
        }
    }

    // --- Courier Applications ---
    route("/api/v1/onboarding/couriers") {

        post {
            val request = call.receive<CreateCourierApplicationRequest>()
            val tenant = call.tenantId
            val userId = UUID.fromString(call.request.headers["X-User-ID"]
                ?: throw IllegalArgumentException("X-User-ID header obrigatorio"))

            val response = onboardingService.createCourierApplication(request, userId, tenant)
            call.respond(HttpStatusCode.Created, response)
        }

        get {
            val tenant = call.tenantId
            val status = call.request.queryParameters["status"]?.let { OnboardingStatus.valueOf(it.uppercase()) }
            val apps = onboardingService.listCourierApplications(tenant, status)
            call.respond(HttpStatusCode.OK, apps)
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            val app = onboardingService.getCourierApplication(id, tenant)
            call.respond(HttpStatusCode.OK, app)
        }

        post("/{id}/documents") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            val request = call.receive<AddDocumentRequest>()
            val doc = onboardingService.addCourierDocument(id, tenant, request)
            call.respond(HttpStatusCode.Created, doc)
        }

        post("/{id}/review") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            val reviewedBy = UUID.fromString(call.request.headers["X-User-ID"]
                ?: throw IllegalArgumentException("X-User-ID header obrigatorio"))
            val request = call.receive<ReviewApplicationRequest>()
            val app = onboardingService.reviewCourierApplication(id, tenant, reviewedBy, request)
            call.respond(HttpStatusCode.OK, app)
        }
    }
}
