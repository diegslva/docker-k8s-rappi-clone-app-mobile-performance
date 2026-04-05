package com.rappiclone.catalog.model

import com.rappiclone.domain.enums.StoreCategory
import kotlinx.serialization.Serializable

// --- Store Requests ---

@Serializable
data class CreateStoreRequest(
    val name: String,
    val description: String? = null,
    val category: StoreCategory,
    val phone: String? = null,
    val email: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val minOrderValue: Double = 0.0,
    val avgPrepTime: Int = 30,
    val deliveryRadiusKm: Double = 5.0
)

@Serializable
data class UpdateStoreRequest(
    val name: String? = null,
    val description: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val minOrderValue: Double? = null,
    val avgPrepTime: Int? = null,
    val deliveryRadiusKm: Double? = null,
    val isOpen: Boolean? = null
)

// --- Product Requests ---

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String? = null,
    val categoryId: String? = null,
    val price: Double,
    val originalPrice: Double? = null,
    val imageUrl: String? = null,
    val prepTimeMin: Int? = null
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val categoryId: String? = null,
    val price: Double? = null,
    val originalPrice: Double? = null,
    val imageUrl: String? = null,
    val prepTimeMin: Int? = null,
    val isAvailable: Boolean? = null
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val description: String? = null,
    val sortOrder: Int = 0
)

// --- Responses ---

@Serializable
data class StoreResponse(
    val id: String,
    val tenantId: String,
    val name: String,
    val slug: String,
    val description: String?,
    val category: StoreCategory,
    val phone: String?,
    val email: String?,
    val logoUrl: String?,
    val bannerUrl: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val latitude: Double?,
    val longitude: Double?,
    val minOrderValue: Double,
    val avgPrepTime: Int,
    val deliveryRadiusKm: Double,
    val ratingAvg: Double,
    val ratingCount: Int,
    val isOpen: Boolean,
    val isActive: Boolean
)

@Serializable
data class ProductResponse(
    val id: String,
    val storeId: String,
    val categoryId: String?,
    val name: String,
    val slug: String,
    val description: String?,
    val price: Double,
    val originalPrice: Double?,
    val imageUrl: String?,
    val prepTimeMin: Int?,
    val isAvailable: Boolean,
    val modifiers: List<ModifierResponse> = emptyList()
)

@Serializable
data class CategoryResponse(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val sortOrder: Int,
    val productCount: Int = 0
)

@Serializable
data class ModifierResponse(
    val id: String,
    val groupName: String,
    val name: String,
    val price: Double,
    val isRequired: Boolean,
    val maxSelections: Int
)

@Serializable
data class StoreMenuResponse(
    val store: StoreResponse,
    val categories: List<CategoryWithProducts>
)

@Serializable
data class CategoryWithProducts(
    val category: CategoryResponse,
    val products: List<ProductResponse>
)

@Serializable
data class BulkImportResult(
    val totalRows: Int,
    val imported: Int,
    val errors: List<String>
)
