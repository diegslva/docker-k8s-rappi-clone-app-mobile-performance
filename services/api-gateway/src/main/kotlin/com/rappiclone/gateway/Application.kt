package com.rappiclone.gateway

import com.rappiclone.domain.errors.ApiError
import com.rappiclone.gateway.config.loadGatewayConfig
import com.rappiclone.gateway.middleware.createJwtAuthPlugin
import com.rappiclone.gateway.middleware.createRateLimitPlugin
import com.rappiclone.gateway.proxy.ServiceRouter
import com.rappiclone.gateway.proxy.proxyRoutes
import com.rappiclone.infra.health.healthRoutes
import com.rappiclone.infra.metrics.metricsRoutes
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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

private val logger = LoggerFactory.getLogger("com.rappiclone.gateway.Application")

fun main() {
    val config = loadGatewayConfig()
    logger.info("Iniciando API Gateway na porta ${config.port}")

    embeddedServer(Netty, port = config.port) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: com.rappiclone.gateway.config.GatewayConfig = loadGatewayConfig()
) {
    val jsonConfig = Json {
        prettyPrint = false
        isLenient = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Server content negotiation
    install(ContentNegotiation) {
        json(jsonConfig)
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
        mdc("service") { "api-gateway" }
    }

    install(createRateLimitPlugin(config.rateLimit))
    install(createJwtAuthPlugin(config.jwt))

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Erro no gateway", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(code = "GATEWAY_ERROR", message = "Erro interno do gateway")
            )
        }
    }

    // HTTP client pra proxy (sem ContentNegotiation plugin — repassa raw bytes)
    val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 30_000
        }
    }

    val router = ServiceRouter(config.services)

    routing {
        healthRoutes()
        metricsRoutes()
        proxyRoutes(httpClient, router)
    }

    logger.info("API Gateway pronto!")
}
