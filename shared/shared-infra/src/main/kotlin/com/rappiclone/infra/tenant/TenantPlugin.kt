package com.rappiclone.infra.tenant

import com.rappiclone.domain.errors.ApiError
import com.rappiclone.domain.tenant.TenantContext
import com.rappiclone.domain.tenant.TenantContextElement
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.infra.tenant.TenantPlugin")

/**
 * Plugin Ktor que extrai X-Tenant-ID do header e injeta no coroutine context.
 * Todo request (exceto health/metrics) DEVE ter tenant.
 */
val TenantPlugin = createApplicationPlugin(name = "TenantPlugin") {
    val excludedPaths = setOf("/health/live", "/health/ready", "/metrics")

    onCall { call ->
        val path = call.request.local.uri
        if (path in excludedPaths) return@onCall

        val tenantId = call.request.headers["X-Tenant-ID"]
        if (tenantId.isNullOrBlank()) {
            logger.warn("Request sem X-Tenant-ID: $path")
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(code = "TENANT_REQUIRED", message = "Header X-Tenant-ID obrigatorio")
            )
            return@onCall
        }

        val zoneId = call.request.headers["X-Zone-ID"]
        val microRegionId = call.request.headers["X-MicroRegion-ID"]

        val tenantContext = TenantContext(
            tenantId = tenantId,
            zoneId = zoneId,
            microRegionId = microRegionId
        )

        // Injeta no coroutine context pra estar disponivel em todo o pipeline
        val element = TenantContextElement(tenantContext)
        call.attributes.put(TenantAttributeKey, tenantContext)
    }
}

val TenantAttributeKey = io.ktor.util.AttributeKey<TenantContext>("TenantContext")

/**
 * Extension pra acessar o tenant do request atual.
 */
val ApplicationCall.tenantContext: TenantContext
    get() = attributes[TenantAttributeKey]

val ApplicationCall.tenantId: String
    get() = tenantContext.tenantId
