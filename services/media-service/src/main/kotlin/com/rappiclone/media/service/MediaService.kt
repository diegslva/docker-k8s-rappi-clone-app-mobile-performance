package com.rappiclone.media.service

import com.rappiclone.domain.errors.DomainError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.media.model.*
import com.rappiclone.media.repository.MediaRepository
import com.rappiclone.media.storage.ObjectStorage
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.UUID

class MediaService(
    private val mediaRepository: MediaRepository,
    private val objectStorage: ObjectStorage,
    private val imageProcessor: ImageProcessor,
    private val defaultBucket: String = "rappiclone-media"
) {
    private val logger = LoggerFactory.getLogger(MediaService::class.java)

    suspend fun upload(
        tenantId: String,
        ownerId: UUID,
        ownerType: OwnerType,
        category: MediaCategory,
        originalName: String,
        contentType: String,
        fileBytes: ByteArray
    ): MediaResponse {
        // Validacao de tipo
        if (!AllowedContentTypes.isAllowed(contentType)) {
            throw DomainException(
                DomainError.ValidationError("Tipo de arquivo nao permitido: $contentType. Aceitos: ${AllowedContentTypes.ALL}")
            )
        }

        // Validacao de tamanho
        val maxBytes = SizeLimits.maxBytesFor(category)
        if (fileBytes.size > maxBytes) {
            val maxMb = maxBytes / (1024 * 1024)
            throw DomainException(
                DomainError.ValidationError("Arquivo excede o limite de ${maxMb}MB para categoria $category")
            )
        }

        // Garante que o bucket existe
        objectStorage.ensureBucket(defaultBucket)

        // Gera object key unico: tenant/ownerType/ownerId/uuid.ext
        val extension = originalName.substringAfterLast('.', "bin")
        val objectKey = "$tenantId/${ownerType.name.lowercase()}/${ownerId}/${UUID.randomUUID()}.$extension"

        // Upload do arquivo original
        objectStorage.upload(
            bucket = defaultBucket,
            key = objectKey,
            data = ByteArrayInputStream(fileBytes),
            contentType = contentType,
            size = fileBytes.size.toLong()
        )

        val url = objectStorage.getUrl(defaultBucket, objectKey)

        // Dimensoes e thumbnail (apenas pra imagens)
        var width: Int? = null
        var height: Int? = null
        var thumbnailKey: String? = null
        var thumbnailUrl: String? = null

        if (AllowedContentTypes.isImage(contentType)) {
            val dimensions = imageProcessor.readDimensions(fileBytes)
            width = dimensions?.width
            height = dimensions?.height

            val thumbSize = ThumbnailSizes.forCategory(category)
            if (thumbSize != null) {
                val thumbBytes = imageProcessor.generateThumbnail(fileBytes, thumbSize.width, thumbSize.height)
                thumbnailKey = "${objectKey}_thumb.jpeg"
                objectStorage.upload(
                    bucket = defaultBucket,
                    key = thumbnailKey,
                    data = ByteArrayInputStream(thumbBytes),
                    contentType = "image/jpeg",
                    size = thumbBytes.size.toLong()
                )
                thumbnailUrl = objectStorage.getUrl(defaultBucket, thumbnailKey)
            }
        }

        // Salva metadata no banco
        val media = mediaRepository.create(
            tenantId = tenantId,
            ownerId = ownerId,
            ownerType = ownerType,
            category = category,
            originalName = originalName,
            contentType = contentType,
            sizeBytes = fileBytes.size.toLong(),
            bucket = defaultBucket,
            objectKey = objectKey,
            url = url,
            thumbnailKey = thumbnailKey,
            thumbnailUrl = thumbnailUrl,
            width = width,
            height = height
        )

        logger.info("Media uploaded: ${media.id} ($originalName, ${fileBytes.size} bytes, $category) tenant=$tenantId")
        return media
    }

    suspend fun getById(id: String, tenantId: String): MediaResponse {
        return mediaRepository.findById(UUID.fromString(id), tenantId)
            ?: throw DomainException(DomainError.NotFound("Media", id))
    }

    suspend fun getByOwner(ownerId: String, ownerType: OwnerType, tenantId: String): List<MediaResponse> {
        return mediaRepository.findByOwner(UUID.fromString(ownerId), ownerType, tenantId)
    }

    suspend fun delete(id: String, tenantId: String) {
        val deleteInfo = mediaRepository.delete(UUID.fromString(id), tenantId)
            ?: throw DomainException(DomainError.NotFound("Media", id))

        // Remove do object storage
        objectStorage.delete(deleteInfo.bucket, deleteInfo.objectKey)
        if (deleteInfo.thumbnailKey != null) {
            objectStorage.delete(deleteInfo.bucket, deleteInfo.thumbnailKey)
        }

        logger.info("Media deleted: $id (key=${deleteInfo.objectKey}) tenant=$tenantId")
    }
}
