package com.rappiclone.gateway.middleware

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.rappiclone.domain.enums.UserRole
import com.rappiclone.domain.errors.ApiError
import com.rappiclone.gateway.config.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.gateway.middleware.JwtAuth")

/**
 * Dados extraidos do JWT, disponibilizados pra handlers downstream.
 */
data class AuthenticatedUser(
    val userId: String,
    val email: String,
    val role: UserRole
)

val AuthUserKey = io.ktor.util.AttributeKey<AuthenticatedUser>("AuthenticatedUser")

/**
 * Rotas que nao precisam de autenticacao.
 */
val PUBLIC_ROUTES = setOf(
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/health/live",
    "/health/ready",
    "/metrics"
)

/**
 * Plugin Ktor que valida JWT em toda request (exceto public routes).
 * Extrai userId, email, role e injeta como attribute no call.
 */
fun createJwtAuthPlugin(config: JwtConfig) = createApplicationPlugin(name = "JwtAuthMiddleware") {
    val algorithm = Algorithm.HMAC256(config.secret)
    val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .build()

    onCall { call ->
        val path = call.request.local.uri
        if (PUBLIC_ROUTES.any { path.startsWith(it) }) return@onCall

        val authHeader = call.request.headers[HttpHeaders.Authorization]
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Request sem Authorization header: $path")
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError(code = "AUTH_REQUIRED", message = "Authorization header obrigatorio")
            )
            return@onCall
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        try {
            val decoded = verifier.verify(token)

            val userId = decoded.subject
                ?: throw JWTVerificationException("Token sem subject")
            val email = decoded.getClaim("email").asString()
                ?: throw JWTVerificationException("Token sem email")
            val roleStr = decoded.getClaim("role").asString()
                ?: throw JWTVerificationException("Token sem role")

            val role = try {
                UserRole.valueOf(roleStr)
            } catch (e: IllegalArgumentException) {
                throw JWTVerificationException("Role invalida: $roleStr")
            }

            call.attributes.put(AuthUserKey, AuthenticatedUser(userId, email, role))

        } catch (e: JWTVerificationException) {
            logger.warn("JWT invalido em $path: ${e.message}")
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError(code = "AUTH_INVALID_TOKEN", message = "Token invalido ou expirado")
            )
        }
    }
}

/**
 * Extensions pra acessar o usuario autenticado no handler.
 */
val ApplicationCall.authenticatedUser: AuthenticatedUser
    get() = attributes[AuthUserKey]

val ApplicationCall.authenticatedUserOrNull: AuthenticatedUser?
    get() = attributes.getOrNull(AuthUserKey)

val ApplicationCall.userId: String
    get() = authenticatedUser.userId
