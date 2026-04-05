package com.rappiclone.infra.health

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val service: String = "",
    val version: String = "0.1.0"
)

/**
 * Endpoints de health check padrao.
 * /health/live  - indica que o processo esta vivo (liveness probe K8s)
 * /health/ready - indica que o servico esta pronto pra receber requests (readiness probe K8s)
 */
fun Routing.healthRoutes() {
    get("/health/live") {
        call.respond(HttpStatusCode.OK, HealthResponse(status = "UP"))
    }
    get("/health/ready") {
        // TODO: verificar conexoes (DB, Redis, Kafka) quando implementados
        call.respond(HttpStatusCode.OK, HealthResponse(status = "READY"))
    }
}
