package com.rappiclone.search.index

import com.rappiclone.search.model.*

/**
 * Interface abstrata pra operacoes de indice de busca.
 * Implementacao concreta usa Elasticsearch.
 * Permite trocar pra OpenSearch, Meilisearch, etc. sem impacto.
 */
interface SearchIndex {
    suspend fun indexStore(store: StoreDocument)
    suspend fun indexProduct(product: ProductDocument)
    suspend fun deleteStore(storeId: String)
    suspend fun deleteProduct(productId: String)
    suspend fun searchStores(
        query: String,
        tenantId: String,
        latitude: Double?,
        longitude: Double?,
        radiusKm: Double,
        category: String?,
        from: Int,
        size: Int
    ): Pair<Long, List<StoreSearchResult>>

    suspend fun searchProducts(
        query: String,
        tenantId: String,
        from: Int,
        size: Int
    ): Pair<Long, List<ProductSearchResult>>

    suspend fun autocomplete(
        query: String,
        tenantId: String,
        limit: Int
    ): List<String>

    suspend fun ensureIndices()
}
