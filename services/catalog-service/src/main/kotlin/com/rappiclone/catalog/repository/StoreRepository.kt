package com.rappiclone.catalog.repository

import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.catalog.model.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StoreRepository {

    private fun ResultRow.toStoreResponse(): StoreResponse = StoreResponse(
        id = this[StoresTable.id].value.toString(),
        tenantId = this[StoresTable.tenantId],
        name = this[StoresTable.name],
        slug = this[StoresTable.slug],
        description = this[StoresTable.description],
        category = StoreCategory.valueOf(this[StoresTable.category]),
        phone = this[StoresTable.phone],
        email = this[StoresTable.email],
        logoUrl = this[StoresTable.logoUrl],
        bannerUrl = this[StoresTable.bannerUrl],
        neighborhood = this[StoresTable.neighborhood],
        city = this[StoresTable.city],
        state = this[StoresTable.state],
        latitude = this[StoresTable.latitude],
        longitude = this[StoresTable.longitude],
        minOrderValue = this[StoresTable.minOrderValue].toDouble(),
        avgPrepTime = this[StoresTable.avgPrepTime],
        deliveryRadiusKm = this[StoresTable.deliveryRadiusKm].toDouble(),
        ratingAvg = this[StoresTable.ratingAvg].toDouble(),
        ratingCount = this[StoresTable.ratingCount],
        isOpen = this[StoresTable.isOpen],
        isActive = this[StoresTable.isActive]
    )

    suspend fun create(request: CreateStoreRequest, ownerUserId: UUID, tenantId: String): StoreResponse = dbQuery {
        val now = OffsetDateTime.now()
        val slug = request.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

        val id = StoresTable.insertAndGetId {
            it[StoresTable.tenantId] = tenantId
            it[StoresTable.ownerUserId] = ownerUserId
            it[name] = request.name
            it[StoresTable.slug] = slug
            it[description] = request.description
            it[category] = request.category.name
            it[phone] = request.phone
            it[email] = request.email
            it[logoUrl] = request.logoUrl
            it[bannerUrl] = request.bannerUrl
            it[street] = request.street
            it[number] = request.number
            it[complement] = request.complement
            it[neighborhood] = request.neighborhood
            it[city] = request.city
            it[state] = request.state
            it[zipCode] = request.zipCode
            it[latitude] = request.latitude
            it[longitude] = request.longitude
            it[minOrderValue] = BigDecimal.valueOf(request.minOrderValue)
            it[avgPrepTime] = request.avgPrepTime
            it[deliveryRadiusKm] = BigDecimal.valueOf(request.deliveryRadiusKm)
            it[ratingAvg] = BigDecimal.ZERO
            it[createdAt] = now
            it[updatedAt] = now
        }

        StoresTable.selectAll().where { StoresTable.id eq id }.single().toStoreResponse()
    }

    suspend fun findById(id: UUID, tenantId: String): StoreResponse? = dbQuery {
        StoresTable.selectAll()
            .where { (StoresTable.id eq id) and (StoresTable.tenantId eq tenantId) and (StoresTable.isActive eq true) }
            .singleOrNull()?.toStoreResponse()
    }

    suspend fun findByTenant(tenantId: String, category: StoreCategory?, openOnly: Boolean): List<StoreResponse> = dbQuery {
        StoresTable.selectAll()
            .where {
                val conditions = mutableListOf(
                    StoresTable.tenantId eq tenantId,
                    StoresTable.isActive eq true
                )
                if (category != null) conditions.add(StoresTable.category eq category.name)
                if (openOnly) conditions.add(StoresTable.isOpen eq true)
                conditions.reduce { acc, op -> acc and op }
            }
            .orderBy(StoresTable.ratingAvg, SortOrder.DESC)
            .map { it.toStoreResponse() }
    }

    suspend fun update(id: UUID, tenantId: String, request: UpdateStoreRequest): StoreResponse? = dbQuery {
        val updated = StoresTable.update({
            (StoresTable.id eq id) and (StoresTable.tenantId eq tenantId)
        }) { stmt ->
            request.name?.let { stmt[name] = it }
            request.description?.let { stmt[description] = it }
            request.phone?.let { stmt[phone] = it }
            request.email?.let { stmt[email] = it }
            request.logoUrl?.let { stmt[logoUrl] = it }
            request.bannerUrl?.let { stmt[bannerUrl] = it }
            request.minOrderValue?.let { stmt[minOrderValue] = BigDecimal.valueOf(it) }
            request.avgPrepTime?.let { stmt[avgPrepTime] = it }
            request.deliveryRadiusKm?.let { stmt[deliveryRadiusKm] = BigDecimal.valueOf(it) }
            request.isOpen?.let { stmt[isOpen] = it }
            stmt[updatedAt] = OffsetDateTime.now()
        }
        if (updated > 0) findById(id, tenantId) else null
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
