package com.rappiclone.tenant.repository

import com.rappiclone.tenant.model.TenantsTable
import com.rappiclone.tenant.model.TenantResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime

class TenantRepository {

    private fun ResultRow.toTenantResponse(): TenantResponse = TenantResponse(
        id = this[TenantsTable.id].value,
        name = this[TenantsTable.name],
        state = this[TenantsTable.state],
        country = this[TenantsTable.country],
        timezone = this[TenantsTable.timezone],
        currency = this[TenantsTable.currency],
        isActive = this[TenantsTable.isActive],
        serviceFeePct = this[TenantsTable.serviceFeePct].toDouble(),
        minOrderValue = this[TenantsTable.minOrderValue].toDouble()
    )

    suspend fun findById(id: String): TenantResponse? = dbQuery {
        TenantsTable.selectAll()
            .where { TenantsTable.id eq id }
            .map { it.toTenantResponse() }
            .singleOrNull()
    }

    suspend fun findAll(activeOnly: Boolean = false): List<TenantResponse> = dbQuery {
        TenantsTable.selectAll()
            .apply { if (activeOnly) where { TenantsTable.isActive eq true } }
            .orderBy(TenantsTable.name)
            .map { it.toTenantResponse() }
    }

    suspend fun create(
        id: String,
        name: String,
        state: String,
        timezone: String,
        serviceFeePct: Double,
        minOrderValue: Double
    ): TenantResponse = dbQuery {
        val now = OffsetDateTime.now()
        TenantsTable.insert {
            it[TenantsTable.id] = id
            it[TenantsTable.name] = name
            it[TenantsTable.state] = state
            it[TenantsTable.timezone] = timezone
            it[TenantsTable.serviceFeePct] = BigDecimal.valueOf(serviceFeePct)
            it[TenantsTable.minOrderValue] = BigDecimal.valueOf(minOrderValue)
            it[createdAt] = now
            it[updatedAt] = now
        }
        findById(id)!!
    }

    suspend fun update(
        id: String,
        name: String?,
        isActive: Boolean?,
        serviceFeePct: Double?,
        minOrderValue: Double?
    ): TenantResponse? = dbQuery {
        val updated = TenantsTable.update({ TenantsTable.id eq id }) { stmt ->
            name?.let { stmt[TenantsTable.name] = it }
            isActive?.let { stmt[TenantsTable.isActive] = it }
            serviceFeePct?.let { stmt[TenantsTable.serviceFeePct] = BigDecimal.valueOf(it) }
            minOrderValue?.let { stmt[TenantsTable.minOrderValue] = BigDecimal.valueOf(it) }
            stmt[updatedAt] = OffsetDateTime.now()
        }
        if (updated > 0) findById(id) else null
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
