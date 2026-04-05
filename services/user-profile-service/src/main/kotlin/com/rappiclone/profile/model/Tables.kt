package com.rappiclone.profile.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object ProfilesTable : UUIDTable("profiles") {
    val userId = uuid("user_id").uniqueIndex()
    val tenantId = varchar("tenant_id", 100)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val displayName = varchar("display_name", 200).nullable()
    val phone = varchar("phone", 20).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val language = varchar("language", 10).default("pt-BR")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object AddressesTable : UUIDTable("addresses") {
    val profileId = uuid("profile_id").references(ProfilesTable.id)
    val tenantId = varchar("tenant_id", 100)
    val label = varchar("label", 50).default("Casa")
    val street = varchar("street", 255)
    val number = varchar("number", 20)
    val complement = varchar("complement", 100).nullable()
    val neighborhood = varchar("neighborhood", 100)
    val city = varchar("city", 100)
    val state = varchar("state", 2)
    val zipCode = varchar("zip_code", 10)
    val country = varchar("country", 2).default("BR")
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val isDefault = bool("is_default").default(false)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
