package com.rappiclone.search.service

import com.rappiclone.search.index.SearchIndex
import com.rappiclone.search.model.*
import org.slf4j.LoggerFactory

class SearchService(
    private val searchIndex: SearchIndex
) {
    private val logger = LoggerFactory.getLogger(SearchService::class.java)

    suspend fun search(request: SearchRequest): SearchResponse {
        require(request.query.isNotBlank()) { "query nao pode ser vazio" }
        require(request.page >= 1) { "page deve ser >= 1" }
        require(request.size in 1..100) { "size deve estar entre 1 e 100" }

        val from = (request.page - 1) * request.size

        val (storeTotal, stores) = searchIndex.searchStores(
            query = request.query,
            tenantId = request.tenantId,
            latitude = request.latitude,
            longitude = request.longitude,
            radiusKm = request.radiusKm,
            category = request.category?.name,
            from = from,
            size = request.size
        )

        val (productTotal, products) = searchIndex.searchProducts(
            query = request.query,
            tenantId = request.tenantId,
            from = from,
            size = request.size
        )

        logger.debug("Search '${request.query}': $storeTotal stores, $productTotal products (tenant=${request.tenantId})")

        return SearchResponse(
            query = request.query,
            totalResults = storeTotal + productTotal,
            stores = stores,
            products = products,
            page = request.page,
            size = request.size
        )
    }

    suspend fun autocomplete(request: AutocompleteRequest): AutocompleteResponse {
        require(request.query.length >= 2) { "query deve ter pelo menos 2 caracteres" }

        val suggestions = searchIndex.autocomplete(
            query = request.query,
            tenantId = request.tenantId,
            limit = request.limit
        )

        return AutocompleteResponse(suggestions = suggestions)
    }

    suspend fun indexStore(store: StoreDocument) {
        searchIndex.indexStore(store)
        logger.debug("Store indexed: ${store.id} (${store.name})")
    }

    suspend fun indexProduct(product: ProductDocument) {
        searchIndex.indexProduct(product)
        logger.debug("Product indexed: ${product.id} (${product.name})")
    }

    suspend fun removeStore(storeId: String) {
        searchIndex.deleteStore(storeId)
        logger.debug("Store removed from index: $storeId")
    }

    suspend fun removeProduct(productId: String) {
        searchIndex.deleteProduct(productId)
        logger.debug("Product removed from index: $productId")
    }
}
