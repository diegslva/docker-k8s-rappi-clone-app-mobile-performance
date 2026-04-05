package com.rappiclone.search.model

import com.rappiclone.domain.enums.StoreCategory
import kotlinx.serialization.Serializable

// --- Search Requests ---

@Serializable
data class SearchRequest(
    val query: String,
    val tenantId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double = 10.0,
    val category: StoreCategory? = null,
    val page: Int = 1,
    val size: Int = 20
)

@Serializable
data class AutocompleteRequest(
    val query: String,
    val tenantId: String,
    val limit: Int = 5
)

// --- Search Results ---

@Serializable
data class SearchResponse(
    val query: String,
    val totalResults: Long,
    val stores: List<StoreSearchResult>,
    val products: List<ProductSearchResult>,
    val page: Int,
    val size: Int
)

@Serializable
data class StoreSearchResult(
    val id: String,
    val name: String,
    val slug: String,
    val category: StoreCategory,
    val logoUrl: String?,
    val neighborhood: String,
    val city: String,
    val ratingAvg: Double,
    val ratingCount: Int,
    val avgPrepTime: Int,
    val minOrderValue: Double,
    val isOpen: Boolean,
    val distanceKm: Double?,
    val score: Double
)

@Serializable
data class ProductSearchResult(
    val id: String,
    val name: String,
    val storeId: String,
    val storeName: String,
    val price: Double,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val score: Double
)

@Serializable
data class AutocompleteResponse(
    val suggestions: List<String>
)

// --- Index Documents (o que vai pro Elasticsearch) ---

@Serializable
data class StoreDocument(
    val id: String,
    val tenantId: String,
    val name: String,
    val slug: String,
    val description: String?,
    val category: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val latitude: Double?,
    val longitude: Double?,
    val ratingAvg: Double,
    val ratingCount: Int,
    val avgPrepTime: Int,
    val minOrderValue: Double,
    val isOpen: Boolean,
    val isActive: Boolean,
    val logoUrl: String?
)

@Serializable
data class ProductDocument(
    val id: String,
    val tenantId: String,
    val storeId: String,
    val storeName: String,
    val name: String,
    val slug: String,
    val description: String?,
    val price: Double,
    val categoryName: String?,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val isActive: Boolean
)
