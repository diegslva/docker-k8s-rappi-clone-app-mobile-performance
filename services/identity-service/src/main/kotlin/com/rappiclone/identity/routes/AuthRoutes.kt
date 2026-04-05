package com.rappiclone.identity.routes

import com.rappiclone.identity.model.*
import com.rappiclone.identity.service.AuthService
import com.rappiclone.identity.service.JwtService
import com.rappiclone.infra.tenant.tenantId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService, jwtService: JwtService) {
    route("/api/v1/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val tenant = call.tenantId
            val response = authService.register(request, tenant)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val response = authService.refresh(request.refreshToken)
            call.respond(HttpStatusCode.OK, response)
        }

        get("/me") {
            val authHeader = call.request.header(HttpHeaders.Authorization)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, MessageResponse("Token obrigatorio"))

            val token = authHeader.removePrefix("Bearer ").trim()
            val payload = jwtService.verifyAccessToken(token)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, MessageResponse("Token invalido ou expirado"))

            val user = authService.getUser(payload.userId)
            call.respond(HttpStatusCode.OK, user)
        }
    }
}
