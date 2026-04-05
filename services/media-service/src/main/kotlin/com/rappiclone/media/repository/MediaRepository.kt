package com.rappiclone.media.repository

import com.rappiclone.media.model.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.util.UUID

class MediaRepository {

    private fun ResultRow.toMediaResponse(): MediaResponse = MediaResponse(
        id = this[MediaTable.id].value.toString(),
        tenantId = this[MediaTable.tenantId],
        ownerId = this[MediaTable.ownerId].toString(),
        ownerType = OwnerType.valueOf(this[MediaTable.ownerType]),
        category = MediaCategory.valueOf(this[MediaTable.category]),
        originalName = this[MediaTable.originalName],
        contentType = this[MediaTable.contentType],
        sizeBytes = this[MediaTable.sizeBytes],
        url = this[MediaTable.url],
        thumbnailUrl = this[MediaTable.thumbnailUrl],
        width = this[MediaTable.width],
        height = this[MediaTable.height]
    )

    suspend fun create(
        tenantId: String,
        ownerId: UUID,
        ownerType: OwnerType,
        category: MediaCategory,
        originalName: String,
        contentType: String,
        sizeBytes: Long,
        bucket: String,
        objectKey: String,
        url: String,
        thumbnailKey: String?,
        thumbnailUrl: String?,
        width: Int?,
        height: Int?
    ): MediaResponse = dbQuery {
        val id = MediaTable.insertAndGetId {
            it[MediaTable.tenantId] = tenantId
            it[MediaTable.ownerId] = ownerId
            it[MediaTable.ownerType] = ownerType.name
            it[MediaTable.category] = category.name
            it[MediaTable.originalName] = originalName
            it[MediaTable.contentType] = contentType
            it[MediaTable.sizeBytes] = sizeBytes
            it[MediaTable.bucket] = bucket
            it[MediaTable.objectKey] = objectKey
            it[MediaTable.url] = url
            it[MediaTable.thumbnailKey] = thumbnailKey
            it[MediaTable.thumbnailUrl] = thumbnailUrl
            it[MediaTable.width] = width
            it[MediaTable.height] = height
            it[createdAt] = OffsetDateTime.now()
        }

        MediaTable.selectAll()
            .where { MediaTable.id eq id }
            .single()
            .toMediaResponse()
    }

    suspend fun findById(id: UUID, tenantId: String): MediaResponse? = dbQuery {
        MediaTable.selectAll()
            .where { (MediaTable.id eq id) and (MediaTable.tenantId eq tenantId) }
            .singleOrNull()
            ?.toMediaResponse()
    }

    suspend fun findByOwner(ownerId: UUID, ownerType: OwnerType, tenantId: String): List<MediaResponse> = dbQuery {
        MediaTable.selectAll()
            .where {
                (MediaTable.ownerId eq ownerId) and
                (MediaTable.ownerType eq ownerType.name) and
                (MediaTable.tenantId eq tenantId)
            }
            .orderBy(MediaTable.createdAt, SortOrder.DESC)
            .map { it.toMediaResponse() }
    }

    suspend fun delete(id: UUID, tenantId: String): MediaDeleteInfo? = dbQuery {
        val media = MediaTable.selectAll()
            .where { (MediaTable.id eq id) and (MediaTable.tenantId eq tenantId) }
            .singleOrNull() ?: return@dbQuery null

        val info = MediaDeleteInfo(
            bucket = media[MediaTable.bucket],
            objectKey = media[MediaTable.objectKey],
            thumbnailKey = media[MediaTable.thumbnailKey]
        )

        MediaTable.deleteWhere { (MediaTable.id eq id) and (MediaTable.tenantId eq tenantId) }
        info
    }

    data class MediaDeleteInfo(
        val bucket: String,
        val objectKey: String,
        val thumbnailKey: String?
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
