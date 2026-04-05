package com.rappiclone.catalog.repository

import com.rappiclone.catalog.model.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ProductRepository {

    private fun ResultRow.toProductResponse(modifiers: List<ModifierResponse> = emptyList()): ProductResponse =
        ProductResponse(
            id = this[ProductsTable.id].value.toString(),
            storeId = this[ProductsTable.storeId].toString(),
            categoryId = this[ProductsTable.categoryId]?.toString(),
            name = this[ProductsTable.name],
            slug = this[ProductsTable.slug],
            description = this[ProductsTable.description],
            price = this[ProductsTable.price].toDouble(),
            originalPrice = this[ProductsTable.originalPrice]?.toDouble(),
            imageUrl = this[ProductsTable.imageUrl],
            prepTimeMin = this[ProductsTable.prepTimeMin],
            isAvailable = this[ProductsTable.isAvailable],
            modifiers = modifiers
        )

    private fun ResultRow.toCategoryResponse(): CategoryResponse = CategoryResponse(
        id = this[ProductCategoriesTable.id].value.toString(),
        name = this[ProductCategoriesTable.name],
        slug = this[ProductCategoriesTable.slug],
        description = this[ProductCategoriesTable.description],
        sortOrder = this[ProductCategoriesTable.sortOrder]
    )

    private fun ResultRow.toModifierResponse(): ModifierResponse = ModifierResponse(
        id = this[ProductModifiersTable.id].value.toString(),
        groupName = this[ProductModifiersTable.groupName],
        name = this[ProductModifiersTable.name],
        price = this[ProductModifiersTable.price].toDouble(),
        isRequired = this[ProductModifiersTable.isRequired],
        maxSelections = this[ProductModifiersTable.maxSelections]
    )

    // --- Categories ---

    suspend fun createCategory(storeId: UUID, tenantId: String, request: CreateCategoryRequest): CategoryResponse = dbQuery {
        val slug = request.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val id = ProductCategoriesTable.insertAndGetId {
            it[ProductCategoriesTable.storeId] = storeId
            it[ProductCategoriesTable.tenantId] = tenantId
            it[name] = request.name
            it[ProductCategoriesTable.slug] = slug
            it[description] = request.description
            it[sortOrder] = request.sortOrder
            it[createdAt] = OffsetDateTime.now()
        }
        ProductCategoriesTable.selectAll().where { ProductCategoriesTable.id eq id }.single().toCategoryResponse()
    }

    suspend fun findCategoriesByStore(storeId: UUID): List<CategoryResponse> = dbQuery {
        ProductCategoriesTable.selectAll()
            .where { (ProductCategoriesTable.storeId eq storeId) and (ProductCategoriesTable.isActive eq true) }
            .orderBy(ProductCategoriesTable.sortOrder)
            .map { it.toCategoryResponse() }
    }

    // --- Products ---

    suspend fun createProduct(storeId: UUID, tenantId: String, request: CreateProductRequest): ProductResponse = dbQuery {
        val now = OffsetDateTime.now()
        val slug = request.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

        val id = ProductsTable.insertAndGetId {
            it[ProductsTable.storeId] = storeId
            it[categoryId] = request.categoryId?.let { UUID.fromString(it) }
            it[ProductsTable.tenantId] = tenantId
            it[name] = request.name
            it[ProductsTable.slug] = slug
            it[description] = request.description
            it[price] = BigDecimal.valueOf(request.price)
            it[originalPrice] = request.originalPrice?.let { BigDecimal.valueOf(it) }
            it[imageUrl] = request.imageUrl
            it[prepTimeMin] = request.prepTimeMin
            it[createdAt] = now
            it[updatedAt] = now
        }
        ProductsTable.selectAll().where { ProductsTable.id eq id }.single().toProductResponse()
    }

    suspend fun findProductsByStore(storeId: UUID, tenantId: String): List<ProductResponse> = dbQuery {
        ProductsTable.selectAll()
            .where {
                (ProductsTable.storeId eq storeId) and
                (ProductsTable.tenantId eq tenantId) and
                (ProductsTable.isActive eq true)
            }
            .orderBy(ProductsTable.sortOrder)
            .map { row ->
                val productId = row[ProductsTable.id].value
                val modifiers = ProductModifiersTable.selectAll()
                    .where { (ProductModifiersTable.productId eq productId) and (ProductModifiersTable.isActive eq true) }
                    .map { it.toModifierResponse() }
                row.toProductResponse(modifiers)
            }
    }

    suspend fun findProductById(id: UUID, tenantId: String): ProductResponse? = dbQuery {
        val row = ProductsTable.selectAll()
            .where { (ProductsTable.id eq id) and (ProductsTable.tenantId eq tenantId) and (ProductsTable.isActive eq true) }
            .singleOrNull() ?: return@dbQuery null

        val modifiers = ProductModifiersTable.selectAll()
            .where { (ProductModifiersTable.productId eq id) and (ProductModifiersTable.isActive eq true) }
            .map { it.toModifierResponse() }

        row.toProductResponse(modifiers)
    }

    suspend fun updateProduct(id: UUID, tenantId: String, request: UpdateProductRequest): ProductResponse? = dbQuery {
        val updated = ProductsTable.update({
            (ProductsTable.id eq id) and (ProductsTable.tenantId eq tenantId)
        }) { stmt ->
            request.name?.let { stmt[name] = it }
            request.description?.let { stmt[description] = it }
            request.categoryId?.let { stmt[categoryId] = UUID.fromString(it) }
            request.price?.let { stmt[price] = BigDecimal.valueOf(it) }
            request.originalPrice?.let { stmt[originalPrice] = BigDecimal.valueOf(it) }
            request.imageUrl?.let { stmt[imageUrl] = it }
            request.prepTimeMin?.let { stmt[prepTimeMin] = it }
            request.isAvailable?.let { stmt[isAvailable] = it }
            stmt[updatedAt] = OffsetDateTime.now()
        }
        if (updated > 0) findProductById(id, tenantId) else null
    }

    suspend fun bulkCreateProducts(storeId: UUID, tenantId: String, products: List<CreateProductRequest>): Int = dbQuery {
        val now = OffsetDateTime.now()
        var count = 0
        for (request in products) {
            val slug = request.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') + "-${count}"
            ProductsTable.insert {
                it[ProductsTable.storeId] = storeId
                it[categoryId] = request.categoryId?.let { UUID.fromString(it) }
                it[ProductsTable.tenantId] = tenantId
                it[name] = request.name
                it[ProductsTable.slug] = slug
                it[description] = request.description
                it[price] = BigDecimal.valueOf(request.price)
                it[originalPrice] = request.originalPrice?.let { BigDecimal.valueOf(it) }
                it[imageUrl] = request.imageUrl
                it[prepTimeMin] = request.prepTimeMin
                it[createdAt] = now
                it[updatedAt] = now
            }
            count++
        }
        count
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
