package com.rappiclone.search.routes

import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.search.model.*
import com.rappiclone.search.service.SearchService
import com.rappiclone.infra.tenant.tenantId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.searchRoutes(searchService: SearchService) {
    route("/api/v1/search") {

        get {
            val tenant = call.tenantId
            val query = call.request.queryParameters["q"]
                ?: throw IllegalArgumentException("Query param 'q' obrigatorio")
            val latitude = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val longitude = call.request.queryParameters["lng"]?.toDoubleOrNull()
            val radiusKm = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 10.0
            val category = call.request.queryParameters["category"]?.let { StoreCategory.valueOf(it.uppercase()) }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20

            val request = SearchRequest(
                query = query,
                tenantId = tenant,
                latitude = latitude,
                longitude = longitude,
                radiusKm = radiusKm,
                category = category,
                page = page,
                size = size
            )

            val response = searchService.search(request)
            call.respond(HttpStatusCode.OK, response)
        }

        get("/autocomplete") {
            val tenant = call.tenantId
            val query = call.request.queryParameters["q"]
                ?: throw IllegalArgumentException("Query param 'q' obrigatorio")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5

            val request = AutocompleteRequest(query = query, tenantId = tenant, limit = limit)
            val response = searchService.autocomplete(request)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    // Endpoints de indexacao (chamados internamente via Kafka ou gRPC)
    route("/api/v1/search/index") {

        post("/store") {
            val store = call.receive<StoreDocument>()
            searchService.indexStore(store)
            call.respond(HttpStatusCode.OK, mapOf("status" to "indexed"))
        }

        post("/product") {
            val product = call.receive<ProductDocument>()
            searchService.indexProduct(product)
            call.respond(HttpStatusCode.OK, mapOf("status" to "indexed"))
        }

        delete("/store/{storeId}") {
            val storeId = call.parameters["storeId"]!!
            searchService.removeStore(storeId)
            call.respond(HttpStatusCode.OK, mapOf("status" to "removed"))
        }

        delete("/product/{productId}") {
            val productId = call.parameters["productId"]!!
            searchService.removeProduct(productId)
            call.respond(HttpStatusCode.OK, mapOf("status" to "removed"))
        }
    }
}
