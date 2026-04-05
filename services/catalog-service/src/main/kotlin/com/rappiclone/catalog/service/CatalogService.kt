package com.rappiclone.catalog.service

import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.domain.errors.DomainError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.catalog.model.*
import com.rappiclone.catalog.repository.ProductRepository
import com.rappiclone.catalog.repository.StoreRepository
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.UUID

class CatalogService(
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(CatalogService::class.java)

    // --- Stores ---

    suspend fun createStore(request: CreateStoreRequest, ownerUserId: UUID, tenantId: String): StoreResponse {
        require(request.name.isNotBlank()) { "name obrigatorio" }
        require(request.state.length == 2) { "state deve ter 2 caracteres (UF)" }
        require(request.minOrderValue >= 0) { "minOrderValue nao pode ser negativo" }
        require(request.deliveryRadiusKm > 0) { "deliveryRadiusKm deve ser positivo" }

        val store = storeRepository.create(request, ownerUserId, tenantId)
        logger.info("Store criada: ${store.id} (${store.name}) tenant=$tenantId")
        return store
    }

    suspend fun getStore(id: String, tenantId: String): StoreResponse {
        return storeRepository.findById(UUID.fromString(id), tenantId)
            ?: throw DomainException(DomainError.StoreNotFound(id))
    }

    suspend fun listStores(tenantId: String, category: StoreCategory?, openOnly: Boolean = false): List<StoreResponse> {
        return storeRepository.findByTenant(tenantId, category, openOnly)
    }

    suspend fun updateStore(id: String, tenantId: String, request: UpdateStoreRequest): StoreResponse {
        return storeRepository.update(UUID.fromString(id), tenantId, request)
            ?: throw DomainException(DomainError.StoreNotFound(id))
    }

    // --- Categories ---

    suspend fun createCategory(storeId: String, tenantId: String, request: CreateCategoryRequest): CategoryResponse {
        require(request.name.isNotBlank()) { "name obrigatorio" }

        storeRepository.findById(UUID.fromString(storeId), tenantId)
            ?: throw DomainException(DomainError.StoreNotFound(storeId))

        return productRepository.createCategory(UUID.fromString(storeId), tenantId, request)
    }

    suspend fun listCategories(storeId: String, tenantId: String): List<CategoryResponse> {
        storeRepository.findById(UUID.fromString(storeId), tenantId)
            ?: throw DomainException(DomainError.StoreNotFound(storeId))

        return productRepository.findCategoriesByStore(UUID.fromString(storeId))
    }

    // --- Products ---

    suspend fun createProduct(storeId: String, tenantId: String, request: CreateProductRequest): ProductResponse {
        require(request.name.isNotBlank()) { "name obrigatorio" }
        require(request.price > 0) { "price deve ser positivo" }

        storeRepository.findById(UUID.fromString(storeId), tenantId)
            ?: throw DomainException(DomainError.StoreNotFound(storeId))

        val product = productRepository.createProduct(UUID.fromString(storeId), tenantId, request)
        logger.info("Produto criado: ${product.id} (${product.name}) na loja $storeId")
        return product
    }

    suspend fun getProduct(id: String, tenantId: String): ProductResponse {
        return productRepository.findProductById(UUID.fromString(id), tenantId)
            ?: throw DomainException(DomainError.ProductNotFound(id))
    }

    suspend fun listProducts(storeId: String, tenantId: String): List<ProductResponse> {
        return productRepository.findProductsByStore(UUID.fromString(storeId), tenantId)
    }

    suspend fun updateProduct(id: String, tenantId: String, request: UpdateProductRequest): ProductResponse {
        return productRepository.updateProduct(UUID.fromString(id), tenantId, request)
            ?: throw DomainException(DomainError.ProductNotFound(id))
    }

    // --- Menu (aggregated) ---

    suspend fun getStoreMenu(storeId: String, tenantId: String): StoreMenuResponse {
        val store = getStore(storeId, tenantId)
        val categories = productRepository.findCategoriesByStore(UUID.fromString(storeId))
        val products = productRepository.findProductsByStore(UUID.fromString(storeId), tenantId)

        val categoriesWithProducts = categories.map { cat ->
            CategoryWithProducts(
                category = cat,
                products = products.filter { it.categoryId == cat.id }
            )
        }

        // Produtos sem categoria
        val uncategorized = products.filter { it.categoryId == null }
        val allCategories = if (uncategorized.isNotEmpty()) {
            categoriesWithProducts + CategoryWithProducts(
                category = CategoryResponse("uncategorized", "Outros", "outros", null, 999),
                products = uncategorized
            )
        } else categoriesWithProducts

        return StoreMenuResponse(store = store, categories = allCategories)
    }

    // --- Bulk Import (CSV) ---

    suspend fun bulkImportProducts(storeId: String, tenantId: String, csvStream: InputStream): BulkImportResult {
        storeRepository.findById(UUID.fromString(storeId), tenantId)
            ?: throw DomainException(DomainError.StoreNotFound(storeId))

        val errors = mutableListOf<String>()
        val products = mutableListOf<CreateProductRequest>()

        csvReader().open(csvStream) {
            readAllWithHeaderAsSequence().forEachIndexed { index, row ->
                try {
                    val name = row["name"] ?: throw IllegalArgumentException("Coluna 'name' obrigatoria")
                    val priceStr = row["price"] ?: throw IllegalArgumentException("Coluna 'price' obrigatoria")
                    val price = priceStr.toDoubleOrNull() ?: throw IllegalArgumentException("Preco invalido: $priceStr")

                    products.add(CreateProductRequest(
                        name = name,
                        description = row["description"],
                        price = price,
                        originalPrice = row["original_price"]?.toDoubleOrNull(),
                        imageUrl = row["image_url"],
                        prepTimeMin = row["prep_time_min"]?.toIntOrNull()
                    ))
                } catch (e: Exception) {
                    errors.add("Linha ${index + 2}: ${e.message}")
                }
            }
        }

        val imported = if (products.isNotEmpty()) {
            productRepository.bulkCreateProducts(UUID.fromString(storeId), tenantId, products)
        } else 0

        logger.info("Bulk import: $imported produtos importados, ${errors.size} erros, loja $storeId")
        return BulkImportResult(totalRows = products.size + errors.size, imported = imported, errors = errors)
    }
}
