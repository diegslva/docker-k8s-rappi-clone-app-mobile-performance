package com.rappiclone.catalog.routes

import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.catalog.model.*
import com.rappiclone.catalog.service.CatalogService
import com.rappiclone.infra.tenant.tenantId
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.catalogRoutes(catalogService: CatalogService) {
    route("/api/v1/stores") {

        post {
            val request = call.receive<CreateStoreRequest>()
            val tenant = call.tenantId
            val ownerUserId = UUID.fromString(call.request.headers["X-User-ID"]
                ?: throw IllegalArgumentException("X-User-ID header obrigatorio"))
            val store = catalogService.createStore(request, ownerUserId, tenant)
            call.respond(HttpStatusCode.Created, store)
        }

        get {
            val tenant = call.tenantId
            val category = call.request.queryParameters["category"]?.let { StoreCategory.valueOf(it.uppercase()) }
            val openOnly = call.request.queryParameters["open"]?.toBoolean() ?: false
            val stores = catalogService.listStores(tenant, category, openOnly)
            call.respond(HttpStatusCode.OK, stores)
        }

        get("/{storeId}") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val store = catalogService.getStore(storeId, tenant)
            call.respond(HttpStatusCode.OK, store)
        }

        put("/{storeId}") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val request = call.receive<UpdateStoreRequest>()
            val store = catalogService.updateStore(storeId, tenant, request)
            call.respond(HttpStatusCode.OK, store)
        }

        // Menu completo (aggregated)
        get("/{storeId}/menu") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val menu = catalogService.getStoreMenu(storeId, tenant)
            call.respond(HttpStatusCode.OK, menu)
        }

        // Categories
        post("/{storeId}/categories") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val request = call.receive<CreateCategoryRequest>()
            val category = catalogService.createCategory(storeId, tenant, request)
            call.respond(HttpStatusCode.Created, category)
        }

        get("/{storeId}/categories") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val categories = catalogService.listCategories(storeId, tenant)
            call.respond(HttpStatusCode.OK, categories)
        }

        // Products
        post("/{storeId}/products") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val request = call.receive<CreateProductRequest>()
            val product = catalogService.createProduct(storeId, tenant, request)
            call.respond(HttpStatusCode.Created, product)
        }

        get("/{storeId}/products") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val products = catalogService.listProducts(storeId, tenant)
            call.respond(HttpStatusCode.OK, products)
        }

        // Bulk import CSV
        post("/{storeId}/products/import") {
            val storeId = call.parameters["storeId"]!!
            val tenant = call.tenantId
            val multipart = call.receiveMultipart()

            var csvStream: java.io.InputStream? = null
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    csvStream = part.streamProvider()
                }
                part.dispose()
            }

            requireNotNull(csvStream) { "Arquivo CSV obrigatorio" }

            val result = catalogService.bulkImportProducts(storeId, tenant, csvStream!!)
            call.respond(HttpStatusCode.OK, result)
        }
    }

    // Product by ID (cross-store)
    route("/api/v1/products") {
        get("/{productId}") {
            val productId = call.parameters["productId"]!!
            val tenant = call.tenantId
            val product = catalogService.getProduct(productId, tenant)
            call.respond(HttpStatusCode.OK, product)
        }

        put("/{productId}") {
            val productId = call.parameters["productId"]!!
            val tenant = call.tenantId
            val request = call.receive<UpdateProductRequest>()
            val product = catalogService.updateProduct(productId, tenant, request)
            call.respond(HttpStatusCode.OK, product)
        }
    }
}
