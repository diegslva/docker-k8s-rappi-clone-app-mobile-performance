package com.rappiclone.media.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object MediaTable : UUIDTable("media") {
    val tenantId = varchar("tenant_id", 100)
    val ownerId = uuid("owner_id")
    val ownerType = varchar("owner_type", 30)
    val category = varchar("category", 30)
    val originalName = varchar("original_name", 255)
    val contentType = varchar("content_type", 100)
    val sizeBytes = long("size_bytes")
    val bucket = varchar("bucket", 100)
    val objectKey = varchar("object_key", 500).uniqueIndex()
    val url = varchar("url", 1000)
    val thumbnailKey = varchar("thumbnail_key", 500).nullable()
    val thumbnailUrl = varchar("thumbnail_url", 1000).nullable()
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
