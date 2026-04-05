package com.rappiclone.tenant.repository

import com.rappiclone.tenant.model.ZoneResponse
import com.rappiclone.tenant.model.ZonesTable
import com.rappiclone.tenant.model.ResolvedLocationResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime

class ZoneRepository {

    private fun ResultRow.toZoneResponse(): ZoneResponse = ZoneResponse(
        id = this[ZonesTable.id].value.toString(),
        tenantId = this[ZonesTable.tenantId],
        name = this[ZonesTable.name],
        slug = this[ZonesTable.slug],
        baseDeliveryFee = this[ZonesTable.baseDeliveryFee].toDouble(),
        maxDeliveryRadiusKm = this[ZonesTable.maxDeliveryRadiusKm].toDouble(),
        estimatedDeliveryMinutes = this[ZonesTable.estimatedDeliveryMinutes],
        isActive = this[ZonesTable.isActive]
    )

    suspend fun findByTenant(tenantId: String): List<ZoneResponse> = dbQuery {
        ZonesTable.selectAll()
            .where { ZonesTable.tenantId eq tenantId }
            .orderBy(ZonesTable.name)
            .map { it.toZoneResponse() }
    }

    suspend fun create(
        tenantId: String,
        name: String,
        slug: String,
        polygonWkt: String,
        baseDeliveryFee: Double,
        maxDeliveryRadiusKm: Double,
        estimatedDeliveryMinutes: Int
    ): ZoneResponse = dbQuery {
        val now = OffsetDateTime.now()
        val id = ZonesTable.insertAndGetId {
            it[ZonesTable.tenantId] = tenantId
            it[ZonesTable.name] = name
            it[ZonesTable.slug] = slug
            it[ZonesTable.baseDeliveryFee] = BigDecimal.valueOf(baseDeliveryFee)
            it[ZonesTable.maxDeliveryRadiusKm] = BigDecimal.valueOf(maxDeliveryRadiusKm)
            it[ZonesTable.estimatedDeliveryMinutes] = estimatedDeliveryMinutes
            it[ZonesTable.isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }

        // Inserir polygon via raw SQL (Exposed nao suporta PostGIS nativo)
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(
            "UPDATE zones SET polygon = ST_GeomFromText(?, 4326) WHERE id = ?::uuid",
            false
        )
        stmt.set(1, polygonWkt)
        stmt.set(2, id.value.toString())
        stmt.executeUpdate()

        ZonesTable.selectAll()
            .where { ZonesTable.id eq id }
            .map { it.toZoneResponse() }
            .single()
    }

    /**
     * Resolve lat/lng pra tenant + zona + micro-regiao usando ST_Contains do PostGIS.
     * Retorna null se o ponto nao esta dentro de nenhuma zona.
     */
    suspend fun resolveLocation(latitude: Double, longitude: Double): ResolvedLocationResponse? = dbQuery {
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(
            """
            SELECT
                z.tenant_id,
                z.id::text as zone_id,
                z.name as zone_name,
                mr.id::text as micro_region_id,
                mr.name as micro_region_name
            FROM zones z
            LEFT JOIN micro_regions mr ON ST_Contains(mr.polygon, ST_SetSRID(ST_Point(?, ?), 4326))
                AND mr.is_active = true
            WHERE ST_Contains(z.polygon, ST_SetSRID(ST_Point(?, ?), 4326))
                AND z.is_active = true
            LIMIT 1
            """.trimIndent(),
            false
        )
        stmt.set(1, longitude)
        stmt.set(2, latitude)
        stmt.set(3, longitude)
        stmt.set(4, latitude)

        val rs = stmt.executeQuery()
        if (rs.next()) {
            ResolvedLocationResponse(
                tenantId = rs.getString(1),
                zoneId = rs.getString(2),
                zoneName = rs.getString(3),
                microRegionId = rs.getString(4),
                microRegionName = rs.getString(5)
            )
        } else null
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
