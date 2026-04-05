package com.rappiclone.catalog.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object StoresTable : UUIDTable("stores") {
    val tenantId = varchar("tenant_id", 100)
    val ownerUserId = uuid("owner_user_id")
    val name = varchar("name", 255)
    val slug = varchar("slug", 255)
    val description = text("description").nullable()
    val category = varchar("category", 30)
    val phone = varchar("phone", 20).nullable()
    val email = varchar("email", 255).nullable()
    val logoUrl = varchar("logo_url", 500).nullable()
    val bannerUrl = varchar("banner_url", 500).nullable()
    val street = varchar("street", 255)
    val number = varchar("number", 20)
    val complement = varchar("complement", 100).nullable()
    val neighborhood = varchar("neighborhood", 100)
    val city = varchar("city", 100)
    val state = varchar("state", 2)
    val zipCode = varchar("zip_code", 10)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val minOrderValue = decimal("min_order_value", 10, 2)
    val avgPrepTime = integer("avg_prep_time").default(30)
    val deliveryRadiusKm = decimal("delivery_radius_km", 5, 2)
    val ratingAvg = decimal("rating_avg", 3, 2)
    val ratingCount = integer("rating_count").default(0)
    val isOpen = bool("is_open").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object ProductCategoriesTable : UUIDTable("product_categories") {
    val storeId = uuid("store_id").references(StoresTable.id)
    val tenantId = varchar("tenant_id", 100)
    val name = varchar("name", 100)
    val slug = varchar("slug", 100)
    val description = text("description").nullable()
    val sortOrder = integer("sort_order").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at")
}

object ProductsTable : UUIDTable("products") {
    val storeId = uuid("store_id").references(StoresTable.id)
    val categoryId = uuid("category_id").references(ProductCategoriesTable.id).nullable()
    val tenantId = varchar("tenant_id", 100)
    val name = varchar("name", 255)
    val slug = varchar("slug", 255)
    val description = text("description").nullable()
    val price = decimal("price", 10, 2)
    val originalPrice = decimal("original_price", 10, 2).nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val prepTimeMin = integer("prep_time_min").nullable()
    val isAvailable = bool("is_available").default(true)
    val isActive = bool("is_active").default(true)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object ProductModifiersTable : UUIDTable("product_modifiers") {
    val productId = uuid("product_id").references(ProductsTable.id)
    val groupName = varchar("group_name", 100)
    val name = varchar("name", 100)
    val price = decimal("price", 10, 2)
    val isRequired = bool("is_required").default(false)
    val maxSelections = integer("max_selections").default(1)
    val sortOrder = integer("sort_order").default(0)
    val isActive = bool("is_active").default(true)
}
