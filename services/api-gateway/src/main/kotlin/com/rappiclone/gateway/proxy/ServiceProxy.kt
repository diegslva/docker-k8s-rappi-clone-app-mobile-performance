package com.rappiclone.gateway.proxy

import com.rappiclone.domain.errors.ApiError
import com.rappiclone.gateway.config.ServiceEndpoints
import com.rappiclone.gateway.middleware.authenticatedUserOrNull
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.gateway.proxy.ServiceProxy")

/**
 * Mapeia prefixo de URL pro endereco do microservice.
 * O Gateway age como reverse proxy: recebe request do mobile,
 * repassa pro microservice correspondente com headers extras
 * (X-Tenant-ID, X-User-ID, X-Correlation-ID).
 */
class ServiceRouter(
    private val endpoints: ServiceEndpoints
) {
    private val routeMap: Map<String, String> = mapOf(
        "/api/v1/auth" to endpoints.identity,
        "/api/v1/users" to endpoints.userProfile,
        "/api/v1/tenants" to endpoints.tenant,
        "/api/v1/location" to endpoints.tenant,
        "/api/v1/stores" to endpoints.catalog,
        "/api/v1/search" to endpoints.search,
        "/api/v1/cart" to endpoints.cart,
        "/api/v1/orders" to endpoints.order,
        "/api/v1/payments" to endpoints.payment,
        "/api/v1/courier" to endpoints.courier,
        "/api/v1/tracking" to endpoints.tracking,
        "/api/v1/notifications" to endpoints.notification,
        "/api/v1/ratings" to endpoints.rating,
        "/api/v1/promotions" to endpoints.promotion,
        "/api/v1/pricing" to endpoints.pricing,
        "/api/v1/geo" to endpoints.geolocation,
        "/api/v1/media" to endpoints.media
    )

    /**
     * Resolve o endereco do microservice baseado no path do request.
     * Retorna o URL base do servico ou null se nao encontrado.
     */
    fun resolve(path: String): String? {
        return routeMap.entries
            .filter { path.startsWith(it.key) }
            .maxByOrNull { it.key.length }
            ?.value
    }
}

/**
 * Proxy handler que repassa requests pros microservices.
 * Adiciona headers de contexto (tenant, user, correlation).
 */
fun Route.proxyRoutes(client: HttpClient, router: ServiceRouter) {
    route("/api/{...}") {
        handle {
            val path = call.request.uri
            val targetBase = router.resolve(path)

            if (targetBase == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiError(code = "ROUTE_NOT_FOUND", message = "Rota nao encontrada: $path")
                )
                return@handle
            }

            val targetUrl = "$targetBase$path"

            try {
                val response: HttpResponse = client.request(targetUrl) {
                    method = call.request.httpMethod

                    // Propaga headers originais
                    call.request.headers.forEach { name, values ->
                        if (name !in setOf(HttpHeaders.Host, HttpHeaders.ContentLength, HttpHeaders.TransferEncoding)) {
                            values.forEach { header(name, it) }
                        }
                    }

                    // Injeta headers de contexto
                    val tenantId = call.request.headers["X-Tenant-ID"]
                    if (tenantId != null) header("X-Tenant-ID", tenantId)

                    val user = call.authenticatedUserOrNull
                    if (user != null) {
                        header("X-User-ID", user.userId)
                        header("X-User-Email", user.email)
                        header("X-User-Role", user.role.name)
                    }

                    val correlationId = call.request.headers[HttpHeaders.XRequestId]
                        ?: call.request.headers["X-Correlation-ID"]
                    if (correlationId != null) header("X-Correlation-ID", correlationId)

                    // Body (pra POST, PUT, PATCH)
                    if (call.request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
                        val body = call.receiveText()
                        setBody(body)
                        contentType(call.request.contentType())
                    }
                }

                // Repassa response do microservice pro cliente
                call.respondBytes(
                    bytes = response.readRawBytes(),
                    contentType = response.contentType(),
                    status = response.status
                )

            } catch (e: Exception) {
                logger.error("Erro ao proxy pra $targetUrl: ${e.message}", e)
                call.respond(
                    HttpStatusCode.BadGateway,
                    ApiError(code = "SERVICE_UNAVAILABLE", message = "Servico temporariamente indisponivel")
                )
            }
        }
    }
}
