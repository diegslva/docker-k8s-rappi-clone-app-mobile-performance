package com.rappiclone.tenant.service

import com.rappiclone.domain.errors.DomainError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.tenant.model.*
import com.rappiclone.tenant.repository.TenantRepository
import com.rappiclone.tenant.repository.ZoneRepository
import org.slf4j.LoggerFactory

class TenantService(
    private val tenantRepository: TenantRepository,
    private val zoneRepository: ZoneRepository
) {
    private val logger = LoggerFactory.getLogger(TenantService::class.java)

    suspend fun createTenant(request: CreateTenantRequest): TenantResponse {
        // Valida value types (strong typing)
        val tenantId = TenantId(request.id)
        val state = BrazilianState(request.state)

        val existing = tenantRepository.findById(tenantId.value)
        if (existing != null) {
            throw DomainException(DomainError.Conflict("Tenant ja existe: ${tenantId.value}"))
        }

        val tenant = tenantRepository.create(
            id = tenantId.value,
            name = request.name,
            state = state.value,
            timezone = request.timezone,
            serviceFeePct = request.serviceFeePct,
            minOrderValue = request.minOrderValue
        )

        logger.info("Tenant criado: ${tenant.id} (${tenant.name}, ${tenant.state})")
        return tenant
    }

    suspend fun getTenant(id: String): TenantResponse {
        val tenantId = TenantId(id)
        return tenantRepository.findById(tenantId.value)
            ?: throw DomainException(DomainError.TenantNotFound(tenantId.value))
    }

    suspend fun listTenants(activeOnly: Boolean = false): List<TenantResponse> =
        tenantRepository.findAll(activeOnly)

    suspend fun updateTenant(id: String, request: UpdateTenantRequest): TenantResponse {
        val tenantId = TenantId(id)
        return tenantRepository.update(
            id = tenantId.value,
            name = request.name,
            isActive = request.isActive,
            serviceFeePct = request.serviceFeePct,
            minOrderValue = request.minOrderValue
        ) ?: throw DomainException(DomainError.TenantNotFound(tenantId.value))
    }

    suspend fun createZone(tenantId: String, request: CreateZoneRequest): ZoneResponse {
        val tid = TenantId(tenantId)
        val slug = ZoneSlug(request.slug)

        // Verifica que tenant existe
        tenantRepository.findById(tid.value)
            ?: throw DomainException(DomainError.TenantNotFound(tid.value))

        val zone = zoneRepository.create(
            tenantId = tid.value,
            name = request.name,
            slug = slug.value,
            polygonWkt = request.polygonWkt,
            baseDeliveryFee = request.baseDeliveryFee,
            maxDeliveryRadiusKm = request.maxDeliveryRadiusKm,
            estimatedDeliveryMinutes = request.estimatedDeliveryMinutes
        )

        logger.info("Zona criada: ${zone.id} (${zone.name}) no tenant ${tid.value}")
        return zone
    }

    suspend fun listZones(tenantId: String): List<ZoneResponse> {
        val tid = TenantId(tenantId)
        return zoneRepository.findByTenant(tid.value)
    }

    /**
     * Resolve coordenadas pra tenant + zona + micro-regiao.
     * Usado pelo API Gateway pra injetar X-Tenant-ID no request.
     */
    suspend fun resolveLocation(request: ResolveLocationRequest): ResolvedLocationResponse {
        val resolved = zoneRepository.resolveLocation(request.latitude, request.longitude)
            ?: throw DomainException(DomainError.LocationOutsideTenant())

        logger.debug("Location resolved: (${request.latitude}, ${request.longitude}) -> tenant=${resolved.tenantId}, zone=${resolved.zoneName}")
        return resolved
    }
}
