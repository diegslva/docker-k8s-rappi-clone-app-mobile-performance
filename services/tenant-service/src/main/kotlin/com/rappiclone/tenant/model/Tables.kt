package com.rappiclone.tenant.model

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/**
 * Tenant = operacao local (cidade/metro).
 * PK e String (slug) ao inves de UUID pra facilitar referencia humana:
 * "londrina-pr", "sao-paulo-sp", "fortaleza-ce".
 */
object TenantsTable : IdTable<String>("tenants") {
    override val id: Column<EntityID<String>> = varchar("id", 100).entityId()
    val name = varchar("name", 255)
    val state = varchar("state", 2)
    val country = varchar("country", 2).default("BR")
    val timezone = varchar("timezone", 50).default("America/Sao_Paulo")
    val currency = varchar("currency", 3).default("BRL")
    val isActive = bool("is_active").default(true)
    val serviceFeePct = decimal("service_fee_pct", 5, 2)
    val minOrderValue = decimal("min_order_value", 10, 2)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object ZonesTable : UUIDTable("zones") {
    val tenantId = varchar("tenant_id", 100).references(TenantsTable.id)
    val name = varchar("name", 255)
    val slug = varchar("slug", 100)
    // polygon armazenado como texto WKT — Exposed nao tem tipo PostGIS nativo
    // Queries geo usam SQL raw com ST_Contains
    val baseDeliveryFee = decimal("base_delivery_fee", 10, 2)
    val maxDeliveryRadiusKm = decimal("max_delivery_radius_km", 5, 2)
    val estimatedDeliveryMinutes = integer("estimated_delivery_minutes")
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object MicroRegionsTable : UUIDTable("micro_regions") {
    val zoneId = uuid("zone_id").references(ZonesTable.id)
    val tenantId = varchar("tenant_id", 100).references(TenantsTable.id)
    val name = varchar("name", 255)
    val slug = varchar("slug", 100)
    val avgTicket = decimal("avg_ticket", 10, 2).nullable()
    val medianIncome = decimal("median_income", 10, 2).nullable()
    val priceElasticity = decimal("price_elasticity", 3, 2).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
