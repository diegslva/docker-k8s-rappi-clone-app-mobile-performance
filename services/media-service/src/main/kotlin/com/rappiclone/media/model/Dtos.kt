package com.rappiclone.media.model

import kotlinx.serialization.Serializable

/**
 * Tipo do dono do media (quem fez upload).
 */
@Serializable
enum class OwnerType {
    USER,
    STORE,
    PRODUCT,
    COURIER,
    ORDER
}

/**
 * Categoria do media (pra que serve).
 */
@Serializable
enum class MediaCategory {
    AVATAR,
    STORE_LOGO,
    STORE_BANNER,
    PRODUCT_PHOTO,
    DOCUMENT,
    PROOF_OF_DELIVERY
}

/**
 * Tipos de conteudo aceitos.
 */
object AllowedContentTypes {
    val IMAGES = setOf("image/jpeg", "image/png", "image/webp")
    val DOCUMENTS = setOf("application/pdf", "image/jpeg", "image/png")
    val ALL = IMAGES + DOCUMENTS

    fun isImage(contentType: String): Boolean = contentType in IMAGES
    fun isDocument(contentType: String): Boolean = contentType in DOCUMENTS
    fun isAllowed(contentType: String): Boolean = contentType in ALL
}

/**
 * Limites de tamanho por categoria.
 */
object SizeLimits {
    const val MAX_IMAGE_BYTES: Long = 10 * 1024 * 1024      // 10MB
    const val MAX_DOCUMENT_BYTES: Long = 25 * 1024 * 1024   // 25MB
    const val MAX_AVATAR_BYTES: Long = 5 * 1024 * 1024      // 5MB

    fun maxBytesFor(category: MediaCategory): Long = when (category) {
        MediaCategory.AVATAR -> MAX_AVATAR_BYTES
        MediaCategory.DOCUMENT -> MAX_DOCUMENT_BYTES
        else -> MAX_IMAGE_BYTES
    }
}

/**
 * Dimensoes de thumbnail por categoria.
 */
object ThumbnailSizes {
    data class Dimensions(val width: Int, val height: Int)

    fun forCategory(category: MediaCategory): Dimensions? = when (category) {
        MediaCategory.AVATAR -> Dimensions(200, 200)
        MediaCategory.STORE_LOGO -> Dimensions(300, 300)
        MediaCategory.STORE_BANNER -> Dimensions(800, 400)
        MediaCategory.PRODUCT_PHOTO -> Dimensions(400, 400)
        MediaCategory.PROOF_OF_DELIVERY -> Dimensions(600, 600)
        MediaCategory.DOCUMENT -> null // docs nao tem thumbnail
    }
}

// --- Responses ---

@Serializable
data class MediaResponse(
    val id: String,
    val tenantId: String,
    val ownerId: String,
    val ownerType: OwnerType,
    val category: MediaCategory,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val url: String,
    val thumbnailUrl: String?,
    val width: Int?,
    val height: Int?
)

@Serializable
data class UploadResult(
    val media: MediaResponse,
    val message: String = "Upload realizado com sucesso"
)
