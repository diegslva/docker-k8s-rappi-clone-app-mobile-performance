package com.rappiclone.infra.ktor

import com.rappiclone.domain.errors.ApiError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.infra.health.healthRoutes
import com.rappiclone.infra.metrics.metricsRoutes
import com.rappiclone.infra.tenant.TenantPlugin
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("com.rappiclone.infra.ktor.KtorSetup")

/**
 * Configura todos os plugins padrao do Ktor que todo servico usa.
 * Chamado no Application.module() de cada servico.
 */
fun Application.installBasePlugins(serviceName: String) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Tenant-ID")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }

    install(CallId) {
        generate { UUID.randomUUID().toString() }
        header(HttpHeaders.XRequestId)
    }

    install(CallLogging) {
        callIdMdc("correlationId")
        mdc("service") { serviceName }
    }

    install(TenantPlugin)

    install(StatusPages) {
        exception<DomainException> { call, cause ->
            val error = cause.error
            logger.warn("Domain error: ${error.code} - ${error.message}")
            call.respond(
                HttpStatusCode.fromValue(error.httpStatus),
                ApiError(code = error.code, message = error.message)
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Validation error: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(code = "VALIDATION_ERROR", message = cause.message ?: "Erro de validacao")
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(code = "INTERNAL_ERROR", message = "Erro interno do servidor")
            )
        }
    }

    routing {
        healthRoutes()
        metricsRoutes()
    }
}
