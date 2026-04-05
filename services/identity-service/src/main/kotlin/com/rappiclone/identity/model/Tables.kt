package com.rappiclone.identity.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255)
    val phone = varchar("phone", 20).nullable()
    val hashedPassword = varchar("hashed_password", 255)
    val role = varchar("role", 20).default("CUSTOMER")
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)
    val tenantId = varchar("tenant_id", 100).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = uuid("user_id").references(UsersTable.id)
    val token = varchar("token", 500)
    val expiresAt = timestampWithTimeZone("expires_at")
    val revoked = bool("revoked").default(false)
    val createdAt = timestampWithTimeZone("created_at")
}

object OtpCodesTable : UUIDTable("otp_codes") {
    val userId = uuid("user_id").references(UsersTable.id)
    val code = varchar("code", 10)
    val purpose = varchar("purpose", 20).default("PHONE_VERIFICATION")
    val expiresAt = timestampWithTimeZone("expires_at")
    val used = bool("used").default(false)
    val createdAt = timestampWithTimeZone("created_at")
}
