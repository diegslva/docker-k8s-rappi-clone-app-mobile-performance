package com.rappiclone.media.routes

import com.rappiclone.media.model.*
import com.rappiclone.media.service.MediaService
import com.rappiclone.infra.tenant.tenantId
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.mediaRoutes(mediaService: MediaService) {
    route("/api/v1/media") {

        post("/upload") {
            val tenant = call.tenantId
            val multipart = call.receiveMultipart()

            var ownerId: UUID? = null
            var ownerType: OwnerType? = null
            var category: MediaCategory? = null
            var fileBytes: ByteArray? = null
            var fileName: String? = null
            var fileContentType: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "ownerId" -> ownerId = UUID.fromString(part.value)
                            "ownerType" -> ownerType = OwnerType.valueOf(part.value.uppercase())
                            "category" -> category = MediaCategory.valueOf(part.value.uppercase())
                        }
                    }
                    is PartData.FileItem -> {
                        fileName = part.originalFileName ?: "unknown"
                        fileContentType = part.contentType?.toString() ?: "application/octet-stream"
                        fileBytes = part.streamProvider().readBytes()
                    }
                    else -> {}
                }
                part.dispose()
            }

            requireNotNull(ownerId) { "ownerId obrigatorio" }
            requireNotNull(ownerType) { "ownerType obrigatorio" }
            requireNotNull(category) { "category obrigatorio" }
            requireNotNull(fileBytes) { "file obrigatorio" }

            val media = mediaService.upload(
                tenantId = tenant,
                ownerId = ownerId!!,
                ownerType = ownerType!!,
                category = category!!,
                originalName = fileName!!,
                contentType = fileContentType!!,
                fileBytes = fileBytes!!
            )

            call.respond(HttpStatusCode.Created, UploadResult(media))
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            val media = mediaService.getById(id, tenant)
            call.respond(HttpStatusCode.OK, media)
        }

        get("/owner/{ownerId}") {
            val ownerId = call.parameters["ownerId"]!!
            val ownerType = OwnerType.valueOf(
                call.request.queryParameters["type"]?.uppercase()
                    ?: throw IllegalArgumentException("Query param 'type' obrigatorio")
            )
            val tenant = call.tenantId
            val medias = mediaService.getByOwner(ownerId, ownerType, tenant)
            call.respond(HttpStatusCode.OK, medias)
        }

        delete("/{id}") {
            val id = call.parameters["id"]!!
            val tenant = call.tenantId
            mediaService.delete(id, tenant)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
